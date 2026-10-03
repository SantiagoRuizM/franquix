package com.franquix.franchises.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.franquix.franchises.domain.exception.DomainValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModelTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-02-01T00:00:00Z");

    @Nested
    class FranchiseModel {

        @Test
        void createTrimsTheNameAndStampsBothTimestamps() {
            Franchise franchise = Franchise.create("  Acme  ", CREATED);

            assertThat(franchise.id()).isNull();
            assertThat(franchise.name()).isEqualTo("Acme");
            assertThat(franchise.createdAt()).isEqualTo(CREATED);
            assertThat(franchise.updatedAt()).isEqualTo(CREATED);
        }

        @Test
        void renameReturnsANewInstanceAndKeepsIdentity() {
            Franchise original = new Franchise("f1", "Acme", CREATED, CREATED);

            Franchise renamed = original.rename(" Acme Global ", LATER);

            assertThat(renamed).isNotSameAs(original);
            assertThat(renamed.id()).isEqualTo("f1");
            assertThat(renamed.name()).isEqualTo("Acme Global");
            assertThat(renamed.createdAt()).isEqualTo(CREATED);
            assertThat(renamed.updatedAt()).isEqualTo(LATER);
            assertThat(original.name()).isEqualTo("Acme");
        }

        @Test
        void rejectsBlankName() {
            assertThatThrownBy(() -> Franchise.create("   ", CREATED))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class BranchModel {

        @Test
        void createKeepsTheFranchiseReference() {
            Branch branch = Branch.create("f1", " Downtown ", CREATED);

            assertThat(branch.franchiseId()).isEqualTo("f1");
            assertThat(branch.name()).isEqualTo("Downtown");
            assertThat(branch.updatedAt()).isEqualTo(CREATED);
        }

        @Test
        void renameOnlyChangesNameAndUpdatedAt() {
            Branch branch = new Branch("b1", "f1", "Downtown", CREATED, CREATED);

            Branch renamed = branch.rename("Uptown", LATER);

            assertThat(renamed).isEqualTo(new Branch("b1", "f1", "Uptown", CREATED, LATER));
        }

        @Test
        void rejectsMissingFranchise() {
            assertThatThrownBy(() -> Branch.create(null, "Downtown", CREATED))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessage("franchiseId is required");
        }
    }

    @Nested
    class ProductModel {

        @Test
        void createValidatesAndStoresStock() {
            Product product = Product.create("b1", " Coffee ", 10, CREATED);

            assertThat(product.branchId()).isEqualTo("b1");
            assertThat(product.name()).isEqualTo("Coffee");
            assertThat(product.stock()).isEqualTo(10);
        }

        @Test
        void withStockOnlyChangesStockAndUpdatedAt() {
            Product product = new Product("p1", "b1", "Coffee", 10, CREATED, CREATED);

            assertThat(product.withStock(0, LATER))
                    .isEqualTo(new Product("p1", "b1", "Coffee", 0, CREATED, LATER));
        }

        @Test
        void renameOnlyChangesNameAndUpdatedAt() {
            Product product = new Product("p1", "b1", "Coffee", 10, CREATED, CREATED);

            assertThat(product.rename("Tea", LATER))
                    .isEqualTo(new Product("p1", "b1", "Tea", 10, CREATED, LATER));
        }

        @Test
        void rejectsNegativeStock() {
            Product product = new Product("p1", "b1", "Coffee", 10, CREATED, CREATED);

            assertThatThrownBy(() -> product.withStock(-5, LATER))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsMissingBranch() {
            assertThatThrownBy(() -> Product.create(" ", "Coffee", 1, CREATED))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessage("branchId is required");
        }
    }

    @Nested
    class ReadModels {

        @Test
        void branchTopProductCombinesBranchAndProduct() {
            Branch branch = new Branch("b1", "f1", "Downtown", CREATED, CREATED);
            Product product = new Product("p1", "b1", "Coffee", 42, CREATED, CREATED);

            assertThat(BranchTopProduct.of(branch, product))
                    .isEqualTo(new BranchTopProduct("b1", "Downtown", "p1", "Coffee", 42));
        }

        @Test
        void overviewsAreDefensivelyCopied() {
            Branch branch = new Branch("b1", "f1", "Downtown", CREATED, CREATED);
            List<Product> products = new ArrayList<>();
            BranchOverview branchOverview = new BranchOverview(branch, products);
            List<BranchOverview> branches = new ArrayList<>(List.of(branchOverview));
            FranchiseOverview overview =
                    new FranchiseOverview(new Franchise("f1", "Acme", CREATED, CREATED), branches);

            products.add(new Product("p1", "b1", "Coffee", 1, CREATED, CREATED));
            branches.clear();

            assertThat(branchOverview.products()).isEmpty();
            assertThat(overview.branches()).containsExactly(branchOverview);
            assertThatThrownBy(() -> overview.branches().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
