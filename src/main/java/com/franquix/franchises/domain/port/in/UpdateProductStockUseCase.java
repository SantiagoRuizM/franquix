package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Product;
import reactor.core.publisher.Mono;

/** Sets the stock of a product. */
@FunctionalInterface
public interface UpdateProductStockUseCase {

    Mono<Product> updateStock(String franchiseId, String branchId, String productId, int stock);
}
