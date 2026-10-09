package com.franquix.franchises.infrastructure.adapter.mongo.repository;

import com.franquix.franchises.infrastructure.adapter.mongo.document.ProductDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface ProductMongoRepository extends ReactiveMongoRepository<ProductDocument, String> {

    Mono<ProductDocument> findByIdAndBranchId(String id, String branchId);

    /** {@code find({branchId}).sort({stock: -1, name: 1}).limit(1)}, served by the (branchId, stock, name) index. */
    Mono<ProductDocument> findFirstByBranchIdOrderByStockDescNameAsc(String branchId);
}
