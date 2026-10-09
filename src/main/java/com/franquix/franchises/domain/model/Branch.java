package com.franquix.franchises.domain.model;

/**
 * A branch of a franchise. Immutable: every change returns a new instance.
 *
 * @param id          identifier, {@code null} until persisted
 * @param franchiseId owning franchise
 * @param name        name, unique within the franchise (trimmed, 1-100 chars)
 */
public record Branch(String id, String franchiseId, String name) {

    public Branch {
        franchiseId = DomainRules.requireReference("franchiseId", franchiseId);
        name = DomainRules.requireValidName("name", name);
    }

    public static Branch create(String franchiseId, String name) {
        return new Branch(null, franchiseId, name);
    }
}
