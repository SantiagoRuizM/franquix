package com.franquix.franchises.application.usecase;

import static com.franquix.franchises.application.usecase.Fixtures.BRANCH_ID;
import static com.franquix.franchises.application.usecase.Fixtures.CLOCK;
import static com.franquix.franchises.application.usecase.Fixtures.CREATED;
import static com.franquix.franchises.application.usecase.Fixtures.FRANCHISE_ID;
import static com.franquix.franchises.application.usecase.Fixtures.NOW;
import static com.franquix.franchises.application.usecase.Fixtures.PRODUCT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductUseCasesTest {

    @Mock
    private FranchiseRepositoryPort franchises;
    @Mock
    private BranchRepositoryPort branches;
    @Mock
    private ProductRepositoryPort products;

    private void givenBranchExists() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.branch()));
    }

    private void givenBranchMissing() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.empty());
    }

    @Nested
    class AddProduct {

        private ProductService useCase;

        @BeforeEach
        void setUp() {
            useCase = new ProductService(franchises, branches, products, CLOCK);
        }

        @Test
        void createsANewProductUnderTheBranch() {
            Product toCreate = new Product(null, BRANCH_ID, "Coffee", 25, NOW, NOW);
            Product created = new Product(PRODUCT_ID, BRANCH_ID, "Coffee", 25, NOW, NOW);
            givenBranchExists();
            when(products.create(toCreate)).thenReturn(Mono.just(created));

            StepVerifier.create(useCase.add(FRANCHISE_ID, BRANCH_ID, " Coffee", 25))
                    .expectNext(created)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheBranchDoesNotExist() {
            givenBranchMissing();

            StepVerifier.create(useCase.add(FRANCHISE_ID, BRANCH_ID, "Coffee", 25))
                    .expectError(NotFoundException.class)
                    .verify();
            verify(products, never()).create(any());
        }

        @Test
        void rejectsNegativeStock() {
            givenBranchExists();

            StepVerifier.create(useCase.add(FRANCHISE_ID, BRANCH_ID, "Coffee", -1))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(products, never()).create(any());
        }

        @Test
        void propagatesDuplicateNameErrors() {
            givenBranchExists();
            when(products.create(any())).thenReturn(Mono.error(DuplicateNameException.product("Coffee")));

            StepVerifier.create(useCase.add(FRANCHISE_ID, BRANCH_ID, "Coffee", 1))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }
    }

    @Nested
    class DeleteProduct {

        private ProductService useCase;

        @BeforeEach
        void setUp() {
            useCase = new ProductService(franchises, branches, products, CLOCK);
        }

        @Test
        void deletesTheProductOfTheBranch() {
            givenBranchExists();
            when(products.deleteByIdAndBranchId(PRODUCT_ID, BRANCH_ID)).thenReturn(Mono.just(true));

            StepVerifier.create(useCase.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                    .verifyComplete();
        }

        @Test
        void failsWhenNothingWasDeleted() {
            givenBranchExists();
            when(products.deleteByIdAndBranchId(PRODUCT_ID, BRANCH_ID)).thenReturn(Mono.just(false));

            StepVerifier.create(useCase.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                    .expectErrorMessage("Product 'product-1' not found in branch 'branch-1'")
                    .verify();
        }

        @Test
        void failsWithoutDeletingWhenTheBranchIsNotInTheFranchise() {
            givenBranchMissing();

            StepVerifier.create(useCase.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                    .expectError(NotFoundException.class)
                    .verify();
            verify(products, never()).deleteByIdAndBranchId(any(), any());
        }
    }

    @Nested
    class UpdateProductStock {

        private ProductService useCase;

        @BeforeEach
        void setUp() {
            useCase = new ProductService(franchises, branches, products, CLOCK);
        }

        @Test
        void updatesTheStockAtomically() {
            Product updated = new Product(PRODUCT_ID, BRANCH_ID, "Coffee", 99, CREATED, NOW);
            givenBranchExists();
            when(products.updateStock(PRODUCT_ID, BRANCH_ID, 99, NOW)).thenReturn(Mono.just(updated));

            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 99))
                    .expectNext(updated)
                    .verifyComplete();
        }

        @Test
        void acceptsZeroStock() {
            Product updated = new Product(PRODUCT_ID, BRANCH_ID, "Coffee", 0, CREATED, NOW);
            givenBranchExists();
            when(products.updateStock(PRODUCT_ID, BRANCH_ID, 0, NOW)).thenReturn(Mono.just(updated));

            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 0))
                    .expectNext(updated)
                    .verifyComplete();
        }

        @Test
        void rejectsNegativeStockWithoutTouchingTheRepositories() {
            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, -3))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(franchises, never()).findById(any());
            verify(products, never()).updateStock(any(), any(), anyInt(), any());
        }

        @Test
        void failsWhenTheProductDoesNotExistOrWasDeleted() {
            givenBranchExists();
            when(products.updateStock(PRODUCT_ID, BRANCH_ID, 3, NOW)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 3))
                    .expectErrorMessage("Product 'product-1' not found in branch 'branch-1'")
                    .verify();
        }

        @Test
        void failsWhenTheBranchIsNotInTheFranchise() {
            givenBranchMissing();

            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 3))
                    .expectError(NotFoundException.class)
                    .verify();
            verify(products, never()).updateStock(any(), any(), anyInt(), any());
        }
    }

    @Nested
    class RenameProduct {

        private ProductService useCase;

        @BeforeEach
        void setUp() {
            useCase = new ProductService(franchises, branches, products, CLOCK);
        }

        @Test
        void renamesAtomically() {
            Product renamed = new Product(PRODUCT_ID, BRANCH_ID, "Espresso", 10, CREATED, NOW);
            givenBranchExists();
            when(products.rename(PRODUCT_ID, BRANCH_ID, "Espresso", NOW)).thenReturn(Mono.just(renamed));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Espresso"))
                    .expectNext(renamed)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheProductDoesNotExist() {
            givenBranchExists();
            when(products.rename(PRODUCT_ID, BRANCH_ID, "Espresso", NOW)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Espresso"))
                    .expectError(NotFoundException.class)
                    .verify();
        }

        @Test
        void propagatesDuplicateNameErrors() {
            givenBranchExists();
            when(products.rename(PRODUCT_ID, BRANCH_ID, "Tea", NOW))
                    .thenReturn(Mono.error(DuplicateNameException.product("Tea")));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Tea"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }

        @Test
        void rejectsAnInvalidNameWithoutTouchingTheRepositories() {
            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "   "))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(products, never()).rename(any(), any(), any(), any());
        }
    }
}
