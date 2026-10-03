package com.franquix.franchises.infrastructure.entrypoint.rest.mapper;

import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.BranchOverview;
import com.franquix.franchises.domain.model.BranchTopProduct;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.FranchiseOverview;
import com.franquix.franchises.domain.model.PageResult;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.BranchResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.FranchiseDetailResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.FranchisePageResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.FranchiseResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.ProductResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.TopStockProductResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.UserResponse;

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

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.email(), user.name(), user.role(), user.active(),
                user.createdAt(), user.updatedAt());
    }

    public static TopStockProductResponse toResponse(BranchTopProduct top) {
        return new TopStockProductResponse(
                top.branchId(), top.branchName(), top.productId(), top.productName(), top.stock());
    }

    public static FranchisePageResponse toResponse(PageResult<Franchise> page) {
        return new FranchisePageResponse(
                page.items().stream().map(RestMapper::toResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public static FranchiseDetailResponse toResponse(FranchiseOverview overview) {
        Franchise franchise = overview.franchise();
        return new FranchiseDetailResponse(
                franchise.id(), franchise.name(), franchise.createdAt(), franchise.updatedAt(),
                overview.branches().stream().map(RestMapper::toBranchDetail).toList());
    }

    private static FranchiseDetailResponse.BranchDetail toBranchDetail(BranchOverview overview) {
        Branch branch = overview.branch();
        return new FranchiseDetailResponse.BranchDetail(
                branch.id(), branch.name(), branch.createdAt(), branch.updatedAt(),
                overview.products().stream().map(RestMapper::toResponse).toList());
    }
}
