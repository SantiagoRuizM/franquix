package com.franquix.franchises.infrastructure.adapter.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Building blocks for field-level updates. They use {@code $set} on single fields, never upsert, and
 * filter on the parent id. An update can therefore neither recreate a deleted document nor
 * overwrite a field another request changed in the meantime.
 */
final class AtomicUpdates {

    static final FindAndModifyOptions RETURN_NEW_NO_UPSERT = FindAndModifyOptions.options()
            .returnNew(true)
            .upsert(false);

    private AtomicUpdates() {
    }

    static Query byId(String id) {
        return Query.query(where("_id").is(new ObjectId(id)));
    }

    static Query byIdAndParent(String id, String parentField, String parentId) {
        return Query.query(where("_id").is(new ObjectId(id)).and(parentField).is(new ObjectId(parentId)));
    }

    static Update rename(String name, Instant updatedAt) {
        return new Update().set("name", name).set("updatedAt", updatedAt);
    }

    static Update stock(int stock, Instant updatedAt) {
        return new Update().set("stock", stock).set("updatedAt", updatedAt);
    }
}
