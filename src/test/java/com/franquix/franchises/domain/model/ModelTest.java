package com.franquix.franchises.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.franquix.franchises.domain.exception.DomainValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModelTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Nested
    class FranchiseModel {

        @Test
        void createTrimsTheNameAndStampsBothTimestamps() {
            Franchise franchise = Franchise.create("  Acme Coffee ", CREATED);

            assertThat(franchise.id()).isNull();
            assertThat(franchise.name()).isEqualTo("Acme Coffee");
            assertThat(franchise.createdAt()).isEqualTo(CREATED);
            assertThat(franchise.updatedAt()).isEqualTo(CREATED);
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
        void rejectsNegativeStock() {
            assertThatThrownBy(() -> Product.create("b1", "Coffee", -5, CREATED))
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
    }
}
