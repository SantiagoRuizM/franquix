package com.franquix.franchises.infrastructure.adapter.mongo;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.infrastructure.adapter.mongo.mapper.MongoDocumentMapper;
import com.franquix.franchises.infrastructure.adapter.mongo.repository.FranchiseMongoRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MongoFranchiseRepositoryAdapter implements FranchiseRepositoryPort {

    private final FranchiseMongoRepository repository;

    public MongoFranchiseRepositoryAdapter(FranchiseMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return repository.save(MongoDocumentMapper.toDocument(franchise))
                .map(MongoDocumentMapper::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.franchise(franchise.name()));
    }

    @Override
    public Mono<Franchise> findById(String franchiseId) {
        return ObjectIds.ifValid(franchiseId, repository::findById)
                .map(MongoDocumentMapper::toDomain);
    }
}
