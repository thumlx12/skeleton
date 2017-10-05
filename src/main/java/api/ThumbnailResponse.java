package api;

import com.fasterxml.jackson.annotation.JsonProperty;
import generated.tables.records.ThumbnailsRecord;

public class ThumbnailResponse {
    @JsonProperty
    public Integer id;

    @JsonProperty
    String img;

    public ThumbnailResponse(ThumbnailsRecord dbRecord) {
        this.id = dbRecord.getId();
        this.img = dbRecord.getImg();
    }
}
