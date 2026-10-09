package com.franquix.franchises.application.usecase;

import static com.franquix.franchises.application.usecase.Fixtures.FRANCHISE_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.BranchTopProduct;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TopStockProductsTest {

    @Mock
    private FranchiseRepositoryPort franchises;
    @Mock
    private BranchRepositoryPort branches;
    @Mock
    private ProductRepositoryPort products;

    private FranchiseService useCase;

    @BeforeEach
    void setUp() {
        useCase = new FranchiseService(franchises, branches, products, Fixtures.CLOCK);
    }

    @Test
    void returnsTheTopProductOfEachBranchAndSkipsEmptyBranches() {
        var centro = Fixtures.branch("b1", "Centro");
        var empty = Fixtures.branch("b2", "Empty");
        var norte = Fixtures.branch("b3", "Norte");
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.just(centro, empty, norte));
        when(products.findTopStockByBranchId("b1"))
                .thenReturn(Mono.just(Fixtures.product("p9", "b1", "Coffee", 120)));
        when(products.findTopStockByBranchId("b2")).thenReturn(Mono.empty());
        when(products.findTopStockByBranchId("b3"))
                .thenReturn(Mono.just(Fixtures.product("p4", "b3", "Tea", 85)));

        StepVerifier.create(useCase.getTopStockProducts(FRANCHISE_ID))
                .expectNext(new BranchTopProduct("b1", "Centro", "p9", "Coffee", 120))
                .expectNext(new BranchTopProduct("b3", "Norte", "p4", "Tea", 85))
                .verifyComplete();
    }

    @Test
    void keepsBranchOrderEvenWhenLookupsCompleteOutOfOrder() {
        var first = Fixtures.branch("b1", "A");
        var second = Fixtures.branch("b2", "B");
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.just(first, second));
        when(products.findTopStockByBranchId("b1"))
                .thenReturn(Mono.just(Fixtures.product("p1", "b1", "Slow", 1)).delayElement(Duration.ofMillis(50)));
        when(products.findTopStockByBranchId("b2"))
                .thenReturn(Mono.just(Fixtures.product("p2", "b2", "Fast", 2)));

        StepVerifier.create(useCase.getTopStockProducts(FRANCHISE_ID))
                .expectNextMatches(top -> top.branchId().equals("b1"))
                .expectNextMatches(top -> top.branchId().equals("b2"))
                .verifyComplete();
    }

    @Test
    void returnsAnEmptyListForAFranchiseWithoutBranches() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
        when(branches.findByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getTopStockProducts(FRANCHISE_ID))
                .verifyComplete();
    }

    @Test
    void failsWhenTheFranchiseDoesNotExist() {
        when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.getTopStockProducts(FRANCHISE_ID))
                .expectError(NotFoundException.class)
                .verify();
        verify(branches, never()).findByFranchiseId(anyString());
    }
}
