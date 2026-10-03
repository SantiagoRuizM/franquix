package com.franquix.franchises.domain.model;

/** The product with the highest stock in a branch, together with the branch it belongs to. */
public record BranchTopProduct(
        String branchId,
        String branchName,
        String productId,
        String productName,
        int stock) {

    public static BranchTopProduct of(Branch branch, Product product) {
        return new BranchTopProduct(branch.id(), branch.name(), product.id(), product.name(), product.stock());
    }
}
