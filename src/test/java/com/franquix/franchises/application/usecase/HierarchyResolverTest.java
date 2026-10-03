package com.franquix.franchises.application.usecase;

import static com.franquix.franchises.application.usecase.Fixtures.BRANCH_ID;
import static com.franquix.franchises.application.usecase.Fixtures.FRANCHISE_ID;
import static com.franquix.franchises.application.usecase.Fixtures.PRODUCT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class HierarchyResolverTest {

    @Mock
    private FranchiseRepositoryPort franchises;
    @Mock
    private BranchRepositoryPort branches;
    @Mock
    private ProductRepositoryPort products;

    private HierarchyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new HierarchyResolver(franchises, branches, products);
    }

    @Test
    void requireFranchiseEmitsTheFranchise() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));

        StepVerifier.create(resolver.requireFranchise(FRANCHISE_ID))
                .expectNext(Fixtures.franchise())
                .verifyComplete();
    }

    @Test
    void requireFranchiseFailsWhenMissing() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(resolver.requireFranchise(FRANCHISE_ID))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(NotFoundException.class)
                        .hasMessage("Franchise 'franchise-1' not found"))
                .verify();
    }

    @Test
    void requireBranchDoesNotLookUpTheBranchWhenTheFranchiseIsMissing() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(resolver.requireBranch(FRANCHISE_ID, BRANCH_ID))
                .expectErrorMessage("Franchise 'franchise-1' not found")
                .verify();
        verify(branches, never()).findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID);
    }

    @Test
    void requireBranchFailsWhenTheBranchBelongsToAnotherFranchise() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(resolver.requireBranch(FRANCHISE_ID, BRANCH_ID))
                .expectErrorMessage("Branch 'branch-1' not found in franchise 'franchise-1'")
                .verify();
    }

    @Test
    void requireProductEmitsTheProduct() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.branch()));
        when(products.findByIdAndBranchId(PRODUCT_ID, BRANCH_ID)).thenReturn(Mono.just(Fixtures.product()));

        StepVerifier.create(resolver.requireProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectNext(Fixtures.product())
                .verifyComplete();
    }

    @Test
    void requireProductFailsWhenTheProductBelongsToAnotherBranch() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.branch()));
        when(products.findByIdAndBranchId(PRODUCT_ID, BRANCH_ID)).thenReturn(Mono.empty());

        StepVerifier.create(resolver.requireProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectErrorMessage("Product 'product-1' not found in branch 'branch-1'")
                .verify();
    }
}
