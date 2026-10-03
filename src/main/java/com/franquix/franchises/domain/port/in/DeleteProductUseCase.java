package com.franquix.franchises.domain.port.in;

import reactor.core.publisher.Mono;

/** Removes a product from a branch of the given franchise. */
@FunctionalInterface
public interface DeleteProductUseCase {

    Mono<Void> delete(String franchiseId, String branchId, String productId);
}
