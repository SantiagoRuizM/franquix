package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Product;
import reactor.core.publisher.Mono;

/** Changes the name of a product. */
@FunctionalInterface
public interface RenameProductUseCase {

    Mono<Product> rename(String franchiseId, String branchId, String productId, String newName);
}
