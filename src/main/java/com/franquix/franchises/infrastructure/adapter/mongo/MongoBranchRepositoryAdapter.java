package com.franquix.franchises.infrastructure.adapter.mongo;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.infrastructure.adapter.mongo.mapper.MongoDocumentMapper;
import com.franquix.franchises.infrastructure.adapter.mongo.repository.BranchMongoRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MongoBranchRepositoryAdapter implements BranchRepositoryPort {

    private final BranchMongoRepository repository;

    public MongoBranchRepositoryAdapter(BranchMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Branch> save(Branch branch) {
        return repository.save(MongoDocumentMapper.toDocument(branch))
                .map(MongoDocumentMapper::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.branch(branch.name()));
    }

    @Override
    public Mono<Branch> findByIdAndFranchiseId(String branchId, String franchiseId) {
        if (!ObjectIds.allValid(branchId, franchiseId)) {
            return Mono.empty();
        }
        return repository.findByIdAndFranchiseId(branchId, franchiseId)
                .map(MongoDocumentMapper::toDomain);
    }

    @Override
    public Flux<Branch> findByFranchiseId(String franchiseId) {
        if (!ObjectIds.allValid(franchiseId)) {
            return Flux.empty();
        }
        return repository.findByFranchiseIdOrderByNameAsc(franchiseId)
                .map(MongoDocumentMapper::toDomain);
    }
}
