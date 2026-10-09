package com.franquix.franchises.domain.model;

/**
 * A franchise. Immutable: every change returns a new instance.
 *
 * @param id        identifier, {@code null} until persisted
 * @param name      unique franchise name (trimmed, 1-100 chars)
 */
public record Franchise(String id, String name) {

    public Franchise {
        name = DomainRules.requireValidName("name", name);
    }

    public static Franchise create(String name) {
        return new Franchise(null, name);
    }
}
