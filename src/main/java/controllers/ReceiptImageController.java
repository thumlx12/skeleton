package controllers;

import api.ReceiptSuggestionResponse;
import api.ThumbnailResponse;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import dao.ThumbnailDao;
import generated.tables.records.ThumbnailsRecord;
import org.hibernate.validator.constraints.NotEmpty;

import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Path("/images")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.APPLICATION_JSON)
public class ReceiptImageController {
    private final AnnotateImageRequest.Builder requestBuilder;
    final ThumbnailDao thumbnailDao;

    public ReceiptImageController(ThumbnailDao dao) {
        // DOCUMENT_TEXT_DETECTION is not the best or only OCR method available
        Feature ocrFeature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        this.requestBuilder = AnnotateImageRequest.newBuilder().addFeatures(ocrFeature);
        this.thumbnailDao = dao;

    }

    public static String imgToBase64String(final RenderedImage img, final String formatName) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, formatName, Base64.getEncoder().wrap(os));
            return os.toString(StandardCharsets.ISO_8859_1.name());
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    public static BufferedImage base64StringToImg(final String base64String) {
        try {
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64String)));
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private static BufferedImage cropImage(BufferedImage src, int x, int y, int w, int h) {
        return src.getSubimage(x, y, w, h);
    }

    private static String getThumbnail(BufferedImage src, List<EntityAnnotation> textAnnotations) {
        int upperX = Integer.MAX_VALUE;
        int upperY = Integer.MAX_VALUE;
        int lowerX = Integer.MIN_VALUE;
        int lowerY = Integer.MIN_VALUE;
        for (EntityAnnotation annotation : textAnnotations) {
            upperX = Integer.min(upperX, annotation.getBoundingPoly().getVertices(0).getX());
            upperX = Integer.min(upperX, annotation.getBoundingPoly().getVertices(3).getX());
            upperY = Integer.min(upperY, annotation.getBoundingPoly().getVertices(0).getY());
            upperY = Integer.min(upperY, annotation.getBoundingPoly().getVertices(1).getY());

            lowerX = Integer.max(lowerX, annotation.getBoundingPoly().getVertices(1).getX());
            lowerX = Integer.max(lowerX, annotation.getBoundingPoly().getVertices(2).getX());
            lowerY = Integer.max(lowerY, annotation.getBoundingPoly().getVertices(2).getY());
            lowerY = Integer.max(lowerY, annotation.getBoundingPoly().getVertices(3).getY());
        }

        BufferedImage thumbnail = cropImage(src, upperX, upperY, lowerX - upperX, lowerY - upperY);
        return imgToBase64String(thumbnail, "png");
    }

    /**
     * This borrows heavily from the Google Vision API Docs.  See:
     * https://cloud.google.com/vision/docs/detecting-fulltext
     * <p>
     * YOU SHOULD MODIFY THIS METHOD TO RETURN A ReceiptSuggestionResponse:
     * <p>
     * public class ReceiptSuggestionResponse {
     * String merchantName;
     * String amount;
     * }
     */

    @GET
    public List<ThumbnailResponse> getThumbnails() {
        List<ThumbnailsRecord> thumbnailRecords = thumbnailDao.getAllThumbnails();
        List<ThumbnailResponse> response = thumbnailRecords.stream().map(ThumbnailResponse::new).collect(toList());
        return response;
    }

    @POST
    public ReceiptSuggestionResponse parseReceipt(@NotEmpty String base64EncodedImage) throws Exception {
        Image img = Image.newBuilder().setContent(ByteString.copyFrom(Base64.getDecoder().decode(base64EncodedImage))).build();
        AnnotateImageRequest request = this.requestBuilder.setImage(img).build();
        BufferedImage originImg = base64StringToImg(base64EncodedImage);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse responses = client.batchAnnotateImages(Collections.singletonList(request));
            AnnotateImageResponse res = responses.getResponses(0);

            String merchantName = null;
            BigDecimal amount = null;

            // Your Algo Here!!
            // Sort text annotations by bounding polygon.  Top-most non-decimal text is the merchant
            // bottom-most decimal text is the total amount
            List<EntityAnnotation> textAnnotations = res.getTextAnnotationsList();
            if (!textAnnotations.isEmpty()) {
                String topMostLine = textAnnotations.get(0).getDescription();
                merchantName = topMostLine.split("[\n\r\t]")[0];
            }

            amountFind:
            for (int i = textAnnotations.size() - 1; i >= 0; i--) {
                EntityAnnotation textAnnotation = textAnnotations.get(i);
                String[] currentLineEles = textAnnotation.getDescription().split(" ");
                for (String ele : currentLineEles) {
                    if (ele.matches("$?[0-9]*(\\.[0-9]{2})?")) {
                        if (ele.charAt(0) == '$') {
                            amount = new BigDecimal(ele.substring(1));
                        } else {
                            amount = new BigDecimal(ele);
                        }
                        break amountFind;
                    }
                }
            }

            String thumbnail = getThumbnail(originImg, textAnnotations);
            thumbnailDao.insert(thumbnail);
            return new ReceiptSuggestionResponse(merchantName, amount, thumbnail);
        }
    }
}
