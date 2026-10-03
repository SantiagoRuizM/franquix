package com.franquix.franchises.domain.model;

import java.util.List;

/** Read model: a branch with its products. */
public record BranchOverview(Branch branch, List<Product> products) {

    public BranchOverview {
        products = List.copyOf(products);
    }
}
