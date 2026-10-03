package com.franquix.franchises.infrastructure.adapter.mongo;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import com.franquix.franchises.infrastructure.adapter.mongo.mapper.MongoDocumentMapper;
import com.franquix.franchises.infrastructure.adapter.mongo.repository.ProductMongoRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MongoProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductMongoRepository repository;

    public MongoProductRepositoryAdapter(ProductMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Product> save(Product product) {
        return repository.save(MongoDocumentMapper.toDocument(product))
                .map(MongoDocumentMapper::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.product(product.name()));
    }

    @Override
    public Mono<Product> findByIdAndBranchId(String productId, String branchId) {
        if (!ObjectIds.allValid(productId, branchId)) {
            return Mono.empty();
        }
        return repository.findByIdAndBranchId(productId, branchId)
                .map(MongoDocumentMapper::toDomain);
    }

    @Override
    public Flux<Product> findByBranchId(String branchId) {
        if (!ObjectIds.allValid(branchId)) {
            return Flux.empty();
        }
        return repository.findByBranchIdOrderByNameAsc(branchId)
                .map(MongoDocumentMapper::toDomain);
    }

    @Override
    public Mono<Product> findTopStockByBranchId(String branchId) {
        return ObjectIds.ifValid(branchId, repository::findFirstByBranchIdOrderByStockDescNameAsc)
                .map(MongoDocumentMapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(String productId) {
        return ObjectIds.ifValid(productId, repository::deleteById);
    }
}
