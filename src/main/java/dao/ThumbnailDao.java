package dao;

import generated.tables.records.ThumbnailsRecord;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;

import static generated.Tables.THUMBNAILS;
import static jersey.repackaged.com.google.common.base.Preconditions.checkState;

public class ThumbnailDao {
    DSLContext dsl;

    public ThumbnailDao(Configuration jooqConfig) {
        this.dsl = DSL.using(jooqConfig);
    }

    public void insert(String img) {
        ThumbnailsRecord thumbnailsRecord = dsl
                .insertInto(THUMBNAILS, THUMBNAILS.IMG)
                .values(img)
                .returning(THUMBNAILS.ID)
                .fetchOne();

        checkState(thumbnailsRecord != null && thumbnailsRecord.getId() != null, "Insert failed");
    }

    public List<ThumbnailsRecord> getAllThumbnails() {
        return dsl.selectFrom(THUMBNAILS).fetch();
    }
}
