package com.franquix.franchises.infrastructure.entrypoint.rest.mapper;

import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.BranchTopProduct;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.BranchResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.FranchiseResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.ProductResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.TopStockProductResponse;

/** Maps domain models to the REST response contract. */
public final class RestMapper {

    private RestMapper() {
    }

    public static FranchiseResponse toResponse(Franchise franchise) {
        return new FranchiseResponse(franchise.id(), franchise.name(), franchise.createdAt(), franchise.updatedAt());
    }

    public static BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.id(), branch.franchiseId(), branch.name(), branch.createdAt(), branch.updatedAt());
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id(), product.branchId(), product.name(), product.stock(),
                product.createdAt(), product.updatedAt());
    }

    public static TopStockProductResponse toResponse(BranchTopProduct top) {
        return new TopStockProductResponse(
                top.branchId(), top.branchName(), top.productId(), top.productName(), top.stock());
    }
}
