package dao;

import api.ReceiptResponse;
import generated.tables.Tags;
import generated.tables.records.ReceiptsRecord;
import generated.tables.records.TagsRecord;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static generated.Tables.RECEIPTS;
import static generated.Tables.TAGS;

public class TagDao {

    DSLContext dsl;

    public TagDao(Configuration jooqConfig) {
        this.dsl = DSL.using(jooqConfig);
    }

    public int insert(String tagName, Integer receiptID) {
        TagsRecord ifExist = dsl
                .selectFrom(TAGS)
                .where(TAGS.TAG.eq(tagName).and(TAGS.RECEIPT_ID.eq(receiptID)))
                .fetchOne();
        if (ifExist == null) {
            TagsRecord tagsRecord = dsl
                    .insertInto(TAGS, TAGS.TAG, TAGS.RECEIPT_ID)
                    .values(tagName, receiptID)
                    .returning(TAGS.ID)
                    .fetchOne();
            checkState(tagsRecord != null && tagsRecord.getId() != null, "Insert failed");
            return tagsRecord.getId();
        } else {
            dsl.deleteFrom(TAGS).where(TAGS.TAG.eq(tagName).and(TAGS.RECEIPT_ID.eq(receiptID))).execute();
            return -1;
        }
    }

    public List<ReceiptsRecord> getAllTaggedReceipts(String tagName) {
        List<Integer> taggedReceiptIDs = dsl.selectFrom(TAGS)
                .where(TAGS.TAG.eq(tagName))
                .fetch()
                .map(x -> x.getReceiptId());

        return dsl.selectFrom(RECEIPTS).where(RECEIPTS.ID.in(taggedReceiptIDs)).fetch();
    }
}


