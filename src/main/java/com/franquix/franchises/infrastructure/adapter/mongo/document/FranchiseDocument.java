package com.franquix.franchises.infrastructure.adapter.mongo.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** MongoDB representation of a franchise ({@code franchises} collection). */
@Document(collection = FranchiseDocument.COLLECTION)
public record FranchiseDocument(
        @Id String id,
        @Indexed(unique = true) String name,
        Instant createdAt,
        Instant updatedAt) {

    public static final String COLLECTION = "franchises";
}
