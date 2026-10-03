package com.franquix.franchises.infrastructure.adapter.mongo.repository;

import com.franquix.franchises.infrastructure.adapter.mongo.document.FranchiseDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface FranchiseMongoRepository extends ReactiveMongoRepository<FranchiseDocument, String> {
}
