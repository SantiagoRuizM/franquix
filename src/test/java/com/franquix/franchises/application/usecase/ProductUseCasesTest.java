package com.franquix.franchises.application.usecase;

import static com.franquix.franchises.application.usecase.Fixtures.BRANCH_ID;
import static com.franquix.franchises.application.usecase.Fixtures.CLOCK;
import static com.franquix.franchises.application.usecase.Fixtures.CREATED;
import static com.franquix.franchises.application.usecase.Fixtures.FRANCHISE_ID;
import static com.franquix.franchises.application.usecase.Fixtures.NOW;
import static com.franquix.franchises.application.usecase.Fixtures.PRODUCT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import reactor.test.publisher.PublisherProbe;

@ExtendWith(MockitoExtension.class)
class ProductUseCasesTest {

    @Mock
    private FranchiseRepositoryPort franchises;
    @Mock
    private BranchRepositoryPort branches;
    @Mock
    private ProductRepositoryPort products;

    private HierarchyResolver hierarchy;

    @BeforeEach
    void setUp() {
        hierarchy = new HierarchyResolver(franchises, branches, products);
    }

    private void givenBranchExists() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.branch()));
    }

    private void givenProductExists() {
        givenBranchExists();
        when(products.findByIdAndBranchId(PRODUCT_ID, BRANCH_ID)).thenReturn(Mono.just(Fixtures.product()));
    }

    private void givenProductMissing() {
        givenBranchExists();
        when(products.findByIdAndBranchId(PRODUCT_ID, BRANCH_ID)).thenReturn(Mono.empty());
    }

    @Nested
    class AddProduct {

        private AddProductService useCase;

        @BeforeEach
        void setUp() {
            useCase = new AddProductService(hierarchy, products, CLOCK);
        }

        @Test
        void savesANewProductUnderTheBranch() {
            Product toSave = new Product(null, BRANCH_ID, "Coffee", 25, NOW, NOW);
            Product saved = new Product(PRODUCT_ID, BRANCH_ID, "Coffee", 25, NOW, NOW);
            givenBranchExists();
            when(products.save(toSave)).thenReturn(Mono.just(saved));

            StepVerifier.create(useCase.add(FRANCHISE_ID, BRANCH_ID, " Coffee", 25))
                    .expectNext(saved)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheBranchDoesNotExist() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.add(FRANCHISE_ID, BRANCH_ID, "Coffee", 25))
                    .expectError(NotFoundException.class)
                    .verify();
            verify(products, never()).save(any());
        }

        @Test
        void rejectsNegativeStock() {
            givenBranchExists();

            StepVerifier.create(useCase.add(FRANCHISE_ID, BRANCH_ID, "Coffee", -1))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(products, never()).save(any());
        }

        @Test
        void propagatesDuplicateNameErrors() {
            givenBranchExists();
            when(products.save(any())).thenReturn(Mono.error(DuplicateNameException.product("Coffee")));

            StepVerifier.create(useCase.add(FRANCHISE_ID, BRANCH_ID, "Coffee", 1))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }
    }

    @Nested
    class DeleteProduct {

        private DeleteProductService useCase;

        @BeforeEach
        void setUp() {
            useCase = new DeleteProductService(hierarchy, products);
        }

        @Test
        void deletesAnExistingProduct() {
            PublisherProbe<Void> deletion = PublisherProbe.empty();
            givenProductExists();
            when(products.deleteById(PRODUCT_ID)).thenReturn(deletion.mono());

            StepVerifier.create(useCase.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                    .verifyComplete();
            deletion.assertWasSubscribed();
        }

        @Test
        void failsWhenTheProductIsNotInTheBranch() {
            givenProductMissing();

            StepVerifier.create(useCase.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                    .expectError(NotFoundException.class)
                    .verify();
            verify(products, never()).deleteById(anyString());
        }
    }

    @Nested
    class UpdateProductStock {

        private UpdateProductStockService useCase;

        @BeforeEach
        void setUp() {
            useCase = new UpdateProductStockService(hierarchy, products, CLOCK);
        }

        @Test
        void savesTheNewStock() {
            Product updated = new Product(PRODUCT_ID, BRANCH_ID, "Coffee", 99, CREATED, NOW);
            givenProductExists();
            when(products.save(updated)).thenReturn(Mono.just(updated));

            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 99))
                    .expectNext(updated)
                    .verifyComplete();
        }

        @Test
        void acceptsZeroStock() {
            Product updated = new Product(PRODUCT_ID, BRANCH_ID, "Coffee", 0, CREATED, NOW);
            givenProductExists();
            when(products.save(updated)).thenReturn(Mono.just(updated));

            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 0))
                    .expectNext(updated)
                    .verifyComplete();
        }

        @Test
        void rejectsNegativeStock() {
            givenProductExists();

            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, -3))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(products, never()).save(any());
        }

        @Test
        void failsWhenTheProductDoesNotExist() {
            givenProductMissing();

            StepVerifier.create(useCase.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 3))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }

    @Nested
    class RenameProduct {

        private RenameProductService useCase;

        @BeforeEach
        void setUp() {
            useCase = new RenameProductService(hierarchy, products, CLOCK);
        }

        @Test
        void savesTheRenamedProduct() {
            Product renamed = new Product(PRODUCT_ID, BRANCH_ID, "Espresso", 10, CREATED, NOW);
            givenProductExists();
            when(products.save(renamed)).thenReturn(Mono.just(renamed));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Espresso"))
                    .expectNext(renamed)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheProductDoesNotExist() {
            givenProductMissing();

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Espresso"))
                    .expectError(NotFoundException.class)
                    .verify();
        }

        @Test
        void propagatesDuplicateNameErrors() {
            givenProductExists();
            when(products.save(any())).thenReturn(Mono.error(DuplicateNameException.product("Tea")));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Tea"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }
    }
}
