package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.Product;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Shared test data for use case tests. */
final class Fixtures {

    static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");
    static final Instant NOW = Instant.parse("2026-03-15T12:30:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    static final String FRANCHISE_ID = "franchise-1";
    static final String BRANCH_ID = "branch-1";
    static final String PRODUCT_ID = "product-1";

    private Fixtures() {
    }

    static Franchise franchise() {
        return new Franchise(FRANCHISE_ID, "Acme", CREATED, CREATED);
    }

    static Branch branch() {
        return branch(BRANCH_ID, "Downtown");
    }

    static Branch branch(String id, String name) {
        return new Branch(id, FRANCHISE_ID, name, CREATED, CREATED);
    }

    static Product product() {
        return product(PRODUCT_ID, BRANCH_ID, "Coffee", 10);
    }

    static Product product(String id, String branchId, String name, int stock) {
        return new Product(id, branchId, name, stock, CREATED, CREATED);
    }
}
