package controllers;

import api.ReceiptSuggestionResponse;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.hibernate.validator.constraints.NotEmpty;

import static java.lang.System.out;

@Path("/images")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.APPLICATION_JSON)
public class ReceiptImageController {
    private final AnnotateImageRequest.Builder requestBuilder;

    public ReceiptImageController() {
        // DOCUMENT_TEXT_DETECTION is not the best or only OCR method available
        Feature ocrFeature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        this.requestBuilder = AnnotateImageRequest.newBuilder().addFeatures(ocrFeature);

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
    @POST
    public ReceiptSuggestionResponse parseReceipt(@NotEmpty String base64EncodedImage) throws Exception {
        Image img = Image.newBuilder().setContent(ByteString.copyFrom(Base64.getDecoder().decode(base64EncodedImage))).build();
        AnnotateImageRequest request = this.requestBuilder.setImage(img).build();

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
            return new ReceiptSuggestionResponse(merchantName, amount);
        }
    }
}
