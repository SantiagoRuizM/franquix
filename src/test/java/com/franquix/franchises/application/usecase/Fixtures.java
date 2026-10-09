package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.Product;

/** Shared test data for use case tests. */
final class Fixtures {

    static final String FRANCHISE_ID = "franchise-1";
    static final String BRANCH_ID = "branch-1";
    static final String PRODUCT_ID = "product-1";

    private Fixtures() {
    }

    static Franchise franchise() {
        return new Franchise(FRANCHISE_ID, "Acme");
    }

    static Branch branch() {
        return branch(BRANCH_ID, "Downtown");
    }

    static Branch branch(String id, String name) {
        return new Branch(id, FRANCHISE_ID, name);
    }

    static Product product(String id, String branchId, String name, int stock) {
        return new Product(id, branchId, name, stock);
    }
}
