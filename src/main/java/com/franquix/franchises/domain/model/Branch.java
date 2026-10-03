package com.franquix.franchises.domain.model;

import java.time.Instant;

/**
 * A branch of a franchise. Immutable: every change returns a new instance.
 *
 * @param id          identifier, {@code null} until persisted
 * @param franchiseId owning franchise
 * @param name        name, unique within the franchise (trimmed, 1-100 chars)
 * @param createdAt   creation timestamp
 * @param updatedAt   last modification timestamp
 */
public record Branch(String id, String franchiseId, String name, Instant createdAt, Instant updatedAt) {

    public Branch {
        franchiseId = DomainRules.requireReference("franchiseId", franchiseId);
        name = DomainRules.requireValidName("name", name);
    }

    public static Branch create(String franchiseId, String name, Instant now) {
        return new Branch(null, franchiseId, name, now, now);
    }
}
