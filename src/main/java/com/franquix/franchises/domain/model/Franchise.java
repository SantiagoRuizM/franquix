package com.franquix.franchises.domain.model;

import java.time.Instant;

/**
 * A franchise. Immutable: every change returns a new instance.
 *
 * @param id        identifier, {@code null} until persisted
 * @param name      unique franchise name (trimmed, 1-100 chars)
 * @param createdAt creation timestamp
 * @param updatedAt last modification timestamp
 */
public record Franchise(String id, String name, Instant createdAt, Instant updatedAt) {

    public Franchise {
        name = DomainRules.requireValidName("name", name);
    }

    public static Franchise create(String name, Instant now) {
        return new Franchise(null, name, now, now);
    }

    public Franchise rename(String newName, Instant now) {
        return new Franchise(id, newName, createdAt, now);
    }
}
