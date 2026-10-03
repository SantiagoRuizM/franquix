package com.franquix.franchises.infrastructure.adapter.mongo.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/** MongoDB representation of a branch ({@code branches} collection), referencing its franchise. */
@Document(collection = BranchDocument.COLLECTION)
public record BranchDocument(
        @Id String id,
        @Field(targetType = FieldType.OBJECT_ID) String franchiseId,
        String name,
        Instant createdAt,
        Instant updatedAt) {

    public static final String COLLECTION = "branches";
}
