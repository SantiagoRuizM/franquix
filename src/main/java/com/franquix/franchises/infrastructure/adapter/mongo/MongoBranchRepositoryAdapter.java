package com.franquix.franchises.infrastructure.adapter.mongo;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.infrastructure.adapter.mongo.document.BranchDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.mapper.MongoDocumentMapper;
import com.franquix.franchises.infrastructure.adapter.mongo.repository.BranchMongoRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MongoBranchRepositoryAdapter implements BranchRepositoryPort {

    private final BranchMongoRepository repository;
    private final ReactiveMongoTemplate template;

    public MongoBranchRepositoryAdapter(BranchMongoRepository repository, ReactiveMongoTemplate template) {
        this.repository = repository;
        this.template = template;
    }

    @Override
    public Mono<Branch> create(Branch branch) {
        return template.insert(MongoDocumentMapper.toDocument(branch))
                .map(MongoDocumentMapper::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.branch(branch.name()));
    }

    @Override
    public Mono<Branch> rename(String branchId, String franchiseId, String name) {
        if (!ObjectIds.allValid(branchId, franchiseId)) {
            return Mono.empty();
        }
        return template.findAndModify(AtomicUpdates.byIdAndParent(branchId, "franchiseId", franchiseId),
                        AtomicUpdates.rename(name), AtomicUpdates.RETURN_NEW_NO_UPSERT, BranchDocument.class)
                .map(MongoDocumentMapper::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.branch(name));
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
