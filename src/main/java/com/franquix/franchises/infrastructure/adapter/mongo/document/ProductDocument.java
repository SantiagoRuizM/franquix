package com.franquix.franchises.infrastructure.adapter.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/** MongoDB representation of a product ({@code products} collection), referencing its branch. */
@Document(collection = ProductDocument.COLLECTION)
@CompoundIndex(name = "ux_product_branch_name", def = "{'branchId': 1, 'name': 1}", unique = true)
public record ProductDocument(
        @Id String id,
        @Field(targetType = FieldType.OBJECT_ID) String branchId,
        String name,
        int stock) {

    public static final String COLLECTION = "products";
}
