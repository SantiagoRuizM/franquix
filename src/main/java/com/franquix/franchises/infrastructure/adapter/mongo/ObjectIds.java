package com.franquix.franchises.infrastructure.adapter.mongo;

import java.util.function.Function;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

/**
 * Ids are MongoDB ObjectIds. A malformed id can never match a document, so lookups short-circuit
 * to empty instead of hitting the database (the REST layer already answers 400 for those).
 */
final class ObjectIds {

    private ObjectIds() {
    }

    static <T> Mono<T> ifValid(String id, Function<String, Mono<T>> lookup) {
        return ObjectId.isValid(id) ? lookup.apply(id) : Mono.empty();
    }

    static boolean allValid(String... ids) {
        for (String id : ids) {
            if (!ObjectId.isValid(id)) {
                return false;
            }
        }
        return true;
    }
}
