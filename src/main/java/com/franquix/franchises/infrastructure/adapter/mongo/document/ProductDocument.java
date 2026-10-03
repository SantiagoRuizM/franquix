package com.franquix.franchises.infrastructure.adapter.mongo.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/** MongoDB representation of a product ({@code products} collection), referencing its branch. */
@Document(collection = ProductDocument.COLLECTION)
public record ProductDocument(
        @Id String id,
        @Field(targetType = FieldType.OBJECT_ID) String branchId,
        String name,
        int stock,
        Instant createdAt,
        Instant updatedAt) {

    public static final String COLLECTION = "products";
}
