package com.franquix.franchises.infrastructure.adapter.mongo;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.PageQuery;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.infrastructure.adapter.mongo.document.FranchiseDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.mapper.MongoDocumentMapper;
import com.franquix.franchises.infrastructure.adapter.mongo.repository.FranchiseMongoRepository;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MongoFranchiseRepositoryAdapter implements FranchiseRepositoryPort {

    private final FranchiseMongoRepository repository;
    private final ReactiveMongoTemplate template;

    public MongoFranchiseRepositoryAdapter(FranchiseMongoRepository repository, ReactiveMongoTemplate template) {
        this.repository = repository;
        this.template = template;
    }

    @Override
    public Mono<Franchise> create(Franchise franchise) {
        return template.insert(MongoDocumentMapper.toDocument(franchise))
                .map(MongoDocumentMapper::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.franchise(franchise.name()));
    }

    @Override
    public Mono<Franchise> rename(String franchiseId, String name, Instant updatedAt) {
        if (!ObjectIds.allValid(franchiseId)) {
            return Mono.empty();
        }
        return template.findAndModify(AtomicUpdates.byId(franchiseId), AtomicUpdates.rename(name, updatedAt),
                        AtomicUpdates.RETURN_NEW_NO_UPSERT, FranchiseDocument.class)
                .map(MongoDocumentMapper::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.franchise(name));
    }

    @Override
    public Mono<Franchise> findById(String franchiseId) {
        return ObjectIds.ifValid(franchiseId, repository::findById)
                .map(MongoDocumentMapper::toDomain);
    }

    @Override
    public Flux<Franchise> findPage(PageQuery page) {
        Query query = new Query()
                .with(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("_id")))
                .skip(page.offset())
                .limit(page.size());
        return template.find(query, FranchiseDocument.class).map(MongoDocumentMapper::toDomain);
    }

    @Override
    public Mono<Long> count() {
        return repository.count();
    }
}
