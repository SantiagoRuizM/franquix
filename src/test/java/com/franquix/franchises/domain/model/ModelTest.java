package com.franquix.franchises.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.franquix.franchises.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModelTest {

    @Nested
    class FranchiseModel {

        @Test
        void createTrimsTheName() {
            Franchise franchise = Franchise.create("  Acme Coffee ");

            assertThat(franchise.id()).isNull();
            assertThat(franchise.name()).isEqualTo("Acme Coffee");
        }

        @Test
        void rejectsBlankName() {
            assertThatThrownBy(() -> Franchise.create("   "))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class BranchModel {

        @Test
        void createKeepsTheFranchiseReference() {
            Branch branch = Branch.create("f1", " Downtown ");

            assertThat(branch.franchiseId()).isEqualTo("f1");
            assertThat(branch.name()).isEqualTo("Downtown");
        }

        @Test
        void rejectsMissingFranchise() {
            assertThatThrownBy(() -> Branch.create(null, "Downtown"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessage("franchiseId is required");
        }
    }

    @Nested
    class ProductModel {

        @Test
        void createValidatesAndStoresStock() {
            Product product = Product.create("b1", " Coffee ", 10);

            assertThat(product.branchId()).isEqualTo("b1");
            assertThat(product.name()).isEqualTo("Coffee");
            assertThat(product.stock()).isEqualTo(10);
        }

        @Test
        void rejectsNegativeStock() {
            assertThatThrownBy(() -> Product.create("b1", "Coffee", -5))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsMissingBranch() {
            assertThatThrownBy(() -> Product.create(" ", "Coffee", 1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessage("branchId is required");
        }
    }

    @Nested
    class ReadModels {

        @Test
        void branchTopProductCombinesBranchAndProduct() {
            Branch branch = new Branch("b1", "f1", "Downtown");
            Product product = new Product("p1", "b1", "Coffee", 42);

            assertThat(BranchTopProduct.of(branch, product))
                    .isEqualTo(new BranchTopProduct("b1", "Downtown", "p1", "Coffee", 42));
        }
    }
}
