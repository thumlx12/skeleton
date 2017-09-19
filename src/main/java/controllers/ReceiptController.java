package controllers;

import api.CreateReceiptRequest;
import api.ReceiptResponse;
import dao.ReceiptDao;
import dao.TagDao;
import generated.tables.records.ReceiptsRecord;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Time;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Path("/receipts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReceiptController {
    final ReceiptDao receiptDao;
    final TagDao tagDao;

    public ReceiptController(ReceiptDao receipts, TagDao tags) {
        this.receiptDao = receipts;
        this.tagDao = tags;
    }

    @POST
    public ReceiptResponse createReceipt(@Valid @NotNull CreateReceiptRequest receipt) {
        return new ReceiptResponse(receiptDao.insert(receipt.merchant, receipt.amount));
    }

    @GET
    public List<ReceiptResponse> getReceipts() {
        List<ReceiptsRecord> receiptRecords = receiptDao.getAllReceipts();
        List<ReceiptResponse> response = receiptRecords.stream().map(ReceiptResponse::new).collect(toList());
        for (ReceiptResponse entry : response) {
            entry.tags = tagDao.getTagsForReceipt(entry.id);
        }
        return response;
    }
}
