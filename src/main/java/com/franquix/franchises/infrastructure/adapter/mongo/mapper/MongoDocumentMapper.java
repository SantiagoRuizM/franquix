package com.franquix.franchises.infrastructure.adapter.mongo.mapper;

import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.infrastructure.adapter.mongo.document.BranchDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.FranchiseDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.ProductDocument;

/** Explicit, allocation-only mapping between domain models and MongoDB documents. */
public final class MongoDocumentMapper {

    private MongoDocumentMapper() {
    }

    public static FranchiseDocument toDocument(Franchise franchise) {
        return new FranchiseDocument(
                franchise.id(), franchise.name(), franchise.createdAt(), franchise.updatedAt());
    }

    public static Franchise toDomain(FranchiseDocument document) {
        return new Franchise(document.id(), document.name(), document.createdAt(), document.updatedAt());
    }

    public static BranchDocument toDocument(Branch branch) {
        return new BranchDocument(
                branch.id(), branch.franchiseId(), branch.name(), branch.createdAt(), branch.updatedAt());
    }

    public static Branch toDomain(BranchDocument document) {
        return new Branch(
                document.id(), document.franchiseId(), document.name(), document.createdAt(), document.updatedAt());
    }

    public static ProductDocument toDocument(Product product) {
        return new ProductDocument(
                product.id(), product.branchId(), product.name(), product.stock(),
                product.createdAt(), product.updatedAt());
    }

    public static Product toDomain(ProductDocument document) {
        return new Product(
                document.id(), document.branchId(), document.name(), document.stock(),
                document.createdAt(), document.updatedAt());
    }
}
