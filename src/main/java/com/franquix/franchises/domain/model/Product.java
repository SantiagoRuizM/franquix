package com.franquix.franchises.domain.model;

/**
 * A product offered by a branch. Immutable: every change returns a new instance.
 *
 * @param id        identifier, {@code null} until persisted
 * @param branchId  owning branch
 * @param name      name, unique within the branch (trimmed, 1-100 chars)
 * @param stock     units available, never negative
 */
public record Product(String id, String branchId, String name, int stock) {

    public Product {
        branchId = DomainRules.requireReference("branchId", branchId);
        name = DomainRules.requireValidName("name", name);
        stock = DomainRules.requireValidStock(stock);
    }

    public static Product create(String branchId, String name, int stock) {
        return new Product(null, branchId, name, stock);
    }
}
