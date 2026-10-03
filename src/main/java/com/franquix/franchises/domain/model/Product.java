package com.franquix.franchises.domain.model;

import java.time.Instant;

/**
 * A product offered by a branch. Immutable: every change returns a new instance.
 *
 * @param id        identifier, {@code null} until persisted
 * @param branchId  owning branch
 * @param name      name, unique within the branch (trimmed, 1-100 chars)
 * @param stock     units available, never negative
 * @param createdAt creation timestamp
 * @param updatedAt last modification timestamp
 */
public record Product(String id, String branchId, String name, int stock, Instant createdAt, Instant updatedAt) {

    public Product {
        branchId = DomainRules.requireReference("branchId", branchId);
        name = DomainRules.requireValidName("name", name);
        stock = DomainRules.requireValidStock(stock);
    }

    public static Product create(String branchId, String name, int stock, Instant now) {
        return new Product(null, branchId, name, stock, now, now);
    }
}
