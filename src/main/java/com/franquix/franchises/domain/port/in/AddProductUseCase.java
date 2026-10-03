package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Product;
import reactor.core.publisher.Mono;

/** Adds a new product to a branch of the given franchise. */
@FunctionalInterface
public interface AddProductUseCase {

    Mono<Product> add(String franchiseId, String branchId, String name, int stock);
}
