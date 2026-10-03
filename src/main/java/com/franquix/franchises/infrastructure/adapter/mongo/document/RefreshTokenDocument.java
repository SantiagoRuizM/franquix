package com.franquix.franchises.infrastructure.adapter.mongo.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * MongoDB representation of a refresh token ({@code refresh_tokens} collection). The id is the token
 * hash; a TTL index on {@code expiresAt} lets MongoDB purge expired tokens by itself.
 */
@Document(collection = RefreshTokenDocument.COLLECTION)
public record RefreshTokenDocument(
        @Id String tokenHash,
        @Field(targetType = FieldType.OBJECT_ID) String userId,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt) {

    public static final String COLLECTION = "refresh_tokens";
}
