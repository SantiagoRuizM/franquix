package com.franquix.franchises.infrastructure.adapter.mongo.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** MongoDB representation of a user ({@code users} collection). */
@Document(collection = UserDocument.COLLECTION)
public record UserDocument(
        @Id String id,
        String email,
        String name,
        String passwordHash,
        String role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static final String COLLECTION = "users";
}
