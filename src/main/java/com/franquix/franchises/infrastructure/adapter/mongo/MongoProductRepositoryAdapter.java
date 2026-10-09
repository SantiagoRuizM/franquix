package com.franquix.franchises.infrastructure.adapter.mongo;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import com.franquix.franchises.infrastructure.adapter.mongo.document.ProductDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.mapper.MongoDocumentMapper;
import com.franquix.franchises.infrastructure.adapter.mongo.repository.ProductMongoRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MongoProductRepositoryAdapter implements ProductRepositoryPort {

    private static final String BRANCH_ID = "branchId";

    private final ProductMongoRepository repository;
    private final ReactiveMongoTemplate template;

    public MongoProductRepositoryAdapter(ProductMongoRepository repository, ReactiveMongoTemplate template) {
        this.repository = repository;
        this.template = template;
    }

    @Override
    public Mono<Product> create(Product product) {
        return template.insert(MongoDocumentMapper.toDocument(product))
                .map(MongoDocumentMapper::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.product(product.name()));
    }

    @Override
    public Mono<Product> rename(String productId, String branchId, String name) {
        return modify(productId, branchId, AtomicUpdates.rename(name))
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.product(name));
    }

    @Override
    public Mono<Product> updateStock(String productId, String branchId, int stock) {
        return modify(productId, branchId, AtomicUpdates.stock(stock));
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
    public Mono<Product> findTopStockByBranchId(String branchId) {
        return ObjectIds.ifValid(branchId, repository::findFirstByBranchIdOrderByStockDescNameAsc)
                .map(MongoDocumentMapper::toDomain);
    }

    @Override
    public Mono<Boolean> deleteByIdAndBranchId(String productId, String branchId) {
        if (!ObjectIds.allValid(productId, branchId)) {
            return Mono.just(false);
        }
        return template.remove(AtomicUpdates.byIdAndParent(productId, BRANCH_ID, branchId), ProductDocument.class)
                .map(result -> result.getDeletedCount() > 0);
    }

    private Mono<Product> modify(String productId, String branchId, Update update) {
        if (!ObjectIds.allValid(productId, branchId)) {
            return Mono.empty();
        }
        return template.findAndModify(AtomicUpdates.byIdAndParent(productId, BRANCH_ID, branchId), update,
                        AtomicUpdates.RETURN_NEW_NO_UPSERT, ProductDocument.class)
                .map(MongoDocumentMapper::toDomain);
    }
}
