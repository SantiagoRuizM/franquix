package com.franquix.franchises.infrastructure.adapter.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/** MongoDB representation of a branch ({@code branches} collection), referencing its franchise. */
@Document(collection = BranchDocument.COLLECTION)
@CompoundIndex(name = "ux_branch_franchise_name", def = "{'franchiseId': 1, 'name': 1}", unique = true)
public record BranchDocument(
        @Id String id,
        @Field(targetType = FieldType.OBJECT_ID) String franchiseId,
        String name) {

    public static final String COLLECTION = "branches";
}
