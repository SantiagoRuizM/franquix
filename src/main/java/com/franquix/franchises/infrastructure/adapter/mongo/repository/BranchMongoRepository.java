package com.franquix.franchises.infrastructure.adapter.mongo.repository;

import com.franquix.franchises.infrastructure.adapter.mongo.document.BranchDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BranchMongoRepository extends ReactiveMongoRepository<BranchDocument, String> {

    Mono<BranchDocument> findByIdAndFranchiseId(String id, String franchiseId);

    Flux<BranchDocument> findByFranchiseIdOrderByNameAsc(String franchiseId);
}
