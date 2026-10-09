package com.franquix.franchises.infrastructure.adapter.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.infrastructure.adapter.mongo.document.BranchDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.FranchiseDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.ProductDocument;
import com.franquix.franchises.support.MongoTestcontainersConfiguration;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DataMongoTest
@Import({
        MongoTestcontainersConfiguration.class,
        MongoFranchiseRepositoryAdapter.class,
        MongoBranchRepositoryAdapter.class,
        MongoProductRepositoryAdapter.class
})
class MongoRepositoryAdaptersIT {

    // MongoDB stores milliseconds; truncate so round-tripped values compare equal
    private static final String MISSING_ID = new ObjectId().toHexString();

    @Autowired
    private ReactiveMongoTemplate template;
    @Autowired
    private MongoFranchiseRepositoryAdapter franchises;
    @Autowired
    private MongoBranchRepositoryAdapter branches;
    @Autowired
    private MongoProductRepositoryAdapter products;

    @BeforeEach
    void cleanCollections() {
        // remove documents but keep the indexes created at startup
        Flux.just(FranchiseDocument.COLLECTION, BranchDocument.COLLECTION, ProductDocument.COLLECTION)
                .concatMap(collection -> template.remove(new Query(), collection))
                .blockLast();
    }

    private Franchise saveFranchise(String name) {
        return franchises.create(Franchise.create(name)).block();
    }

    private Branch saveBranch(String franchiseId, String name) {
        return branches.create(Branch.create(franchiseId, name)).block();
    }

    private Product saveProduct(String branchId, String name, int stock) {
        return products.create(Product.create(branchId, name, stock)).block();
    }

    @Test
    void declaresTheUniqueNameIndexes() {
        StepVerifier.create(Flux.just(FranchiseDocument.COLLECTION, BranchDocument.COLLECTION, ProductDocument.COLLECTION)
                        .concatMap(collection -> template.indexOps(collection).getIndexInfo())
                        .filter(IndexInfo::isUnique)
                        .count())
                .expectNext(3L)
                .verifyComplete();
    }

    @Nested
    class Franchises {

        @Test
        void createAssignsAnObjectIdAndFindByIdReadsItBack() {
            Franchise saved = saveFranchise("Acme");

            assertThat(ObjectId.isValid(saved.id())).isTrue();
            StepVerifier.create(franchises.findById(saved.id()))
                    .expectNext(saved)
                    .verifyComplete();
        }

        @Test
        void renameUpdatesOnlyTheName() {
            Franchise saved = saveFranchise("Acme");

            StepVerifier.create(franchises.rename(saved.id(), "Acme Global"))
                    .expectNext(new Franchise(saved.id(), "Acme Global"))
                    .verifyComplete();
        }

        @Test
        void renameIsEmptyForAMissingFranchiseAndCreatesNothing() {
            StepVerifier.create(franchises.rename(MISSING_ID, "Ghost")).verifyComplete();
            StepVerifier.create(franchises.rename("bad", "Ghost")).verifyComplete();
            StepVerifier.create(template.count(new Query(), FranchiseDocument.COLLECTION))
                    .expectNext(0L)
                    .verifyComplete();
        }

        @Test
        void renameToATakenNameIsADuplicate() {
            saveFranchise("Acme");
            Franchise other = saveFranchise("Other");

            StepVerifier.create(franchises.rename(other.id(), "Acme"))
                    .expectErrorMessage("A franchise named 'Acme' already exists")
                    .verify();
        }

        @Test
        void rejectsADuplicateName() {
            saveFranchise("Acme");

            StepVerifier.create(franchises.create(Franchise.create("Acme")))
                    .expectErrorMessage("A franchise named 'Acme' already exists")
                    .verify();
        }

        @Test
        void renamingToTheCurrentNameIsNotADuplicate() {
            Franchise saved = saveFranchise("Acme");

            StepVerifier.create(franchises.rename(saved.id(), "Acme"))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        void findByIdIsEmptyForUnknownOrMalformedIds() {
            StepVerifier.create(franchises.findById(MISSING_ID)).verifyComplete();
            StepVerifier.create(franchises.findById("not-an-object-id")).verifyComplete();
        }
    }

    @Nested
    class Branches {

        @Test
        void storesTheFranchiseReferenceAsAnObjectId() {
            Franchise franchise = saveFranchise("Acme");
            Branch branch = saveBranch(franchise.id(), "Downtown");

            StepVerifier.create(template.findById(new ObjectId(branch.id()), Document.class, BranchDocument.COLLECTION))
                    .assertNext(raw -> assertThat(raw.get("franchiseId")).isEqualTo(new ObjectId(franchise.id())))
                    .verifyComplete();
        }

        @Test
        void findByIdAndFranchiseIdEnforcesOwnership() {
            Franchise acme = saveFranchise("Acme");
            Franchise other = saveFranchise("Other");
            Branch branch = saveBranch(acme.id(), "Downtown");

            StepVerifier.create(branches.findByIdAndFranchiseId(branch.id(), acme.id()))
                    .expectNext(branch)
                    .verifyComplete();
            StepVerifier.create(branches.findByIdAndFranchiseId(branch.id(), other.id())).verifyComplete();
            StepVerifier.create(branches.findByIdAndFranchiseId("bad", acme.id())).verifyComplete();
        }

        @Test
        void findByFranchiseIdReturnsBranchesSortedByName() {
            Franchise franchise = saveFranchise("Acme");
            saveBranch(franchise.id(), "Uptown");
            saveBranch(franchise.id(), "Airport");
            saveBranch(saveFranchise("Other").id(), "Elsewhere");

            StepVerifier.create(branches.findByFranchiseId(franchise.id()).map(Branch::name))
                    .expectNext("Airport", "Uptown")
                    .verifyComplete();
            StepVerifier.create(branches.findByFranchiseId("bad")).verifyComplete();
        }

        @Test
        void branchNamesAreUniquePerFranchiseOnly() {
            Franchise acme = saveFranchise("Acme");
            Franchise other = saveFranchise("Other");
            saveBranch(acme.id(), "Downtown");

            StepVerifier.create(branches.create(Branch.create(other.id(), "Downtown")))
                    .expectNextCount(1)
                    .verifyComplete();
            StepVerifier.create(branches.create(Branch.create(acme.id(), "Downtown")))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }

        @Test
        void renameOnlyAppliesInsideTheOwningFranchise() {
            Franchise acme = saveFranchise("Acme");
            Franchise other = saveFranchise("Other");
            Branch branch = saveBranch(acme.id(), "Downtown");

            StepVerifier.create(branches.rename(branch.id(), other.id(), "Hijacked")).verifyComplete();
            StepVerifier.create(branches.rename(branch.id(), acme.id(), "Uptown"))
                    .assertNext(renamed -> assertThat(renamed.name()).isEqualTo("Uptown"))
                    .verifyComplete();
            StepVerifier.create(branches.rename("bad", acme.id(), "Uptown")).verifyComplete();
        }

        @Test
        void renameToATakenNameIsADuplicate() {
            Franchise acme = saveFranchise("Acme");
            saveBranch(acme.id(), "Downtown");
            Branch uptown = saveBranch(acme.id(), "Uptown");

            StepVerifier.create(branches.rename(uptown.id(), acme.id(), "Downtown"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }
    }

    @Nested
    class Products {

        private Branch branch;

        @BeforeEach
        void setUp() {
            branch = saveBranch(saveFranchise("Acme").id(), "Downtown");
        }

        @Test
        void findByIdAndBranchIdEnforcesOwnership() {
            Branch otherBranch = saveBranch(branch.franchiseId(), "Uptown");
            Product coffee = saveProduct(branch.id(), "Coffee", 5);

            StepVerifier.create(products.findByIdAndBranchId(coffee.id(), branch.id()))
                    .expectNext(coffee)
                    .verifyComplete();
            StepVerifier.create(products.findByIdAndBranchId(coffee.id(), otherBranch.id())).verifyComplete();
            StepVerifier.create(products.findByIdAndBranchId("bad", branch.id())).verifyComplete();
        }

        @Test
        void findTopStockReturnsTheHighestStockAndBreaksTiesByName() {
            saveProduct(branch.id(), "Tea", 50);
            saveProduct(branch.id(), "Coffee", 80);
            saveProduct(branch.id(), "Bagel", 80);
            saveProduct(branch.id(), "Water", 3);

            StepVerifier.create(products.findTopStockByBranchId(branch.id()))
                    .assertNext(top -> assertThat(top.name()).isEqualTo("Bagel"))
                    .verifyComplete();
        }

        @Test
        void findTopStockIsEmptyForABranchWithoutProducts() {
            StepVerifier.create(products.findTopStockByBranchId(branch.id())).verifyComplete();
            StepVerifier.create(products.findTopStockByBranchId("bad")).verifyComplete();
        }

        @Test
        void deleteRemovesTheProductOnlyFromItsBranch() {
            Branch otherBranch = saveBranch(branch.franchiseId(), "Uptown");
            Product coffee = saveProduct(branch.id(), "Coffee", 5);

            StepVerifier.create(products.deleteByIdAndBranchId(coffee.id(), otherBranch.id()))
                    .expectNext(false)
                    .verifyComplete();
            StepVerifier.create(products.deleteByIdAndBranchId(coffee.id(), branch.id()))
                    .expectNext(true)
                    .verifyComplete();
            StepVerifier.create(products.findByIdAndBranchId(coffee.id(), branch.id())).verifyComplete();
            StepVerifier.create(products.deleteByIdAndBranchId(coffee.id(), branch.id()))
                    .expectNext(false)
                    .verifyComplete();
            StepVerifier.create(products.deleteByIdAndBranchId("bad", branch.id()))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        void updatingADeletedProductNeverRecreatesIt() {
            Product coffee = saveProduct(branch.id(), "Coffee", 5);
            products.deleteByIdAndBranchId(coffee.id(), branch.id()).block();

            StepVerifier.create(products.updateStock(coffee.id(), branch.id(), 9)).verifyComplete();
            StepVerifier.create(products.rename(coffee.id(), branch.id(), "Espresso")).verifyComplete();
            StepVerifier.create(template.count(new Query(), ProductDocument.COLLECTION))
                    .expectNext(0L)
                    .verifyComplete();
        }

        @Test
        void stockUpdateAndRenameNeverOverwriteEachOther() {
            Product coffee = saveProduct(branch.id(), "Coffee", 5);

            // each request carries only its own field, so applying them in any order keeps both changes
            products.rename(coffee.id(), branch.id(), "Espresso").block();
            products.updateStock(coffee.id(), branch.id(), 42).block();

            StepVerifier.create(products.findByIdAndBranchId(coffee.id(), branch.id()))
                    .assertNext(found -> {
                        assertThat(found.name()).isEqualTo("Espresso");
                        assertThat(found.stock()).isEqualTo(42);
                    })
                    .verifyComplete();
        }

        @Test
        void updatesOnlyApplyInsideTheOwningBranch() {
            Branch otherBranch = saveBranch(branch.franchiseId(), "Uptown");
            Product coffee = saveProduct(branch.id(), "Coffee", 5);

            StepVerifier.create(products.updateStock(coffee.id(), otherBranch.id(), 99)).verifyComplete();
            StepVerifier.create(products.rename(coffee.id(), otherBranch.id(), "Hijacked")).verifyComplete();
            StepVerifier.create(products.updateStock("bad", branch.id(), 1)).verifyComplete();
            StepVerifier.create(products.findByIdAndBranchId(coffee.id(), branch.id()))
                    .expectNext(coffee)
                    .verifyComplete();
        }

        @Test
        void renameToATakenNameIsADuplicate() {
            saveProduct(branch.id(), "Coffee", 5);
            Product tea = saveProduct(branch.id(), "Tea", 1);

            StepVerifier.create(products.rename(tea.id(), branch.id(), "Coffee"))
                    .expectErrorMessage("A product named 'Coffee' already exists in this branch")
                    .verify();
        }

        @Test
        void productNamesAreUniquePerBranch() {
            saveProduct(branch.id(), "Coffee", 5);

            StepVerifier.create(products.create(Product.create(branch.id(), "Coffee", 1)))
                    .expectErrorMessage("A product named 'Coffee' already exists in this branch")
                    .verify();
        }

        @Test
        void updateStockReturnsTheUpdatedProduct() {
            Product coffee = saveProduct(branch.id(), "Coffee", 5);

            StepVerifier.create(products.updateStock(coffee.id(), branch.id(), 42))
                    .expectNext(new Product(coffee.id(), branch.id(), "Coffee", 42))
                    .verifyComplete();
        }
    }
}
