package api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Represents the result of an OCR parse
 */
public class ReceiptSuggestionResponse {
    @JsonProperty
    public final String merchantName;

    @JsonProperty
    public final BigDecimal amount;

    @JsonProperty
    public final String thumbnail;

    public ReceiptSuggestionResponse(String merchantName, BigDecimal amount, String thumbnail) {
        this.merchantName = merchantName;
        this.amount = amount;
        this.thumbnail = thumbnail;
    }
}
