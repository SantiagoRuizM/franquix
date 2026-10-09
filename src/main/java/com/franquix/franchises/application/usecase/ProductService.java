package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.DomainRules;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.in.ProductUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

/**
 * Resolves the {@code franchise > branch} path first. Product ownership is enforced by the product
 * operations themselves, which always filter on the branch.
 */
public class ProductService implements ProductUseCase {

    private final FranchiseRepositoryPort franchises;
    private final BranchRepositoryPort branches;
    private final ProductRepositoryPort products;

    public ProductService(
            FranchiseRepositoryPort franchises,
            BranchRepositoryPort branches,
            ProductRepositoryPort products) {
        this.franchises = franchises;
        this.branches = branches;
        this.products = products;
    }

    @Override
    public Mono<Product> add(String franchiseId, String branchId, String name, int stock) {
        return requireBranch(franchiseId, branchId)
                .map(branch -> Product.create(branch.id(), name, stock))
                .flatMap(products::create);
    }

    @Override
    public Mono<Void> delete(String franchiseId, String branchId, String productId) {
        return requireBranch(franchiseId, branchId)
                .flatMap(branch -> products.deleteByIdAndBranchId(productId, branch.id()))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(() -> NotFoundException.product(branchId, productId)))
                .then();
    }

    @Override
    public Mono<Product> updateStock(String franchiseId, String branchId, String productId, int stock) {
        return Mono.fromCallable(() -> DomainRules.requireValidStock(stock))
                .flatMap(validStock -> requireBranch(franchiseId, branchId)
                        .flatMap(branch -> products.updateStock(productId, branch.id(), validStock))
                        .switchIfEmpty(Mono.error(() -> NotFoundException.product(branchId, productId))));
    }

    @Override
    public Mono<Product> rename(String franchiseId, String branchId, String productId, String newName) {
        return Mono.fromCallable(() -> DomainRules.requireValidName("name", newName))
                .flatMap(name -> requireBranch(franchiseId, branchId)
                        .flatMap(branch -> products.rename(productId, branch.id(), name))
                        .switchIfEmpty(Mono.error(() -> NotFoundException.product(branchId, productId))));
    }

    private Mono<Branch> requireBranch(String franchiseId, String branchId) {
        return franchises.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)))
                .flatMap(franchise -> branches.findByIdAndFranchiseId(branchId, franchise.id()))
                .switchIfEmpty(Mono.error(() -> NotFoundException.branch(franchiseId, branchId)));
    }
}
