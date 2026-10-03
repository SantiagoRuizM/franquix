package com.franquix.franchises.application.usecase;

import static com.franquix.franchises.application.usecase.Fixtures.CLOCK;
import static com.franquix.franchises.application.usecase.Fixtures.CREATED;
import static com.franquix.franchises.application.usecase.Fixtures.FRANCHISE_ID;
import static com.franquix.franchises.application.usecase.Fixtures.NOW;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.BranchOverview;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.FranchiseOverview;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FranchiseUseCasesTest {

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

    @Nested
    class CreateFranchise {

        private CreateFranchiseService useCase;

        @BeforeEach
        void setUp() {
            useCase = new CreateFranchiseService(franchises, CLOCK);
        }

        @Test
        void savesATrimmedFranchiseStampedWithTheClock() {
            Franchise expectedToSave = new Franchise(null, "Acme", NOW, NOW);
            Franchise saved = new Franchise(FRANCHISE_ID, "Acme", NOW, NOW);
            when(franchises.save(expectedToSave)).thenReturn(Mono.just(saved));

            StepVerifier.create(useCase.create("  Acme "))
                    .expectNext(saved)
                    .verifyComplete();
        }

        @Test
        void propagatesDuplicateNameErrors() {
            when(franchises.save(any())).thenReturn(Mono.error(DuplicateNameException.franchise("Acme")));

            StepVerifier.create(useCase.create("Acme"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }

        @Test
        void failsWithoutTouchingTheRepositoryWhenTheNameIsInvalid() {
            StepVerifier.create(useCase.create("  "))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(franchises, never()).save(any());
        }
    }

    @Nested
    class RenameFranchise {

        private RenameFranchiseService useCase;

        @BeforeEach
        void setUp() {
            useCase = new RenameFranchiseService(hierarchy, franchises, CLOCK);
        }

        @Test
        void savesTheRenamedFranchise() {
            Franchise renamed = new Franchise(FRANCHISE_ID, "Acme Global", CREATED, NOW);
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(franchises.save(renamed)).thenReturn(Mono.just(renamed));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, "Acme Global"))
                    .expectNext(renamed)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheFranchiseDoesNotExist() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.rename(FRANCHISE_ID, "Acme Global"))
                    .expectError(NotFoundException.class)
                    .verify();
            verify(franchises, never()).save(any());
        }

        @Test
        void propagatesDuplicateNameErrors() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(franchises.save(any())).thenReturn(Mono.error(DuplicateNameException.franchise("Other")));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, "Other"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }

        @Test
        void rejectsAnInvalidName() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, ""))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(franchises, never()).save(any());
        }
    }

    @Nested
    class GetFranchise {

        private GetFranchiseService useCase;

        @BeforeEach
        void setUp() {
            useCase = new GetFranchiseService(hierarchy, branches, products);
        }

        @Test
        void assemblesTheWholeTreeKeepingBranchOrder() {
            var downtown = Fixtures.branch("b1", "Downtown");
            var uptown = Fixtures.branch("b2", "Uptown");
            var coffee = Fixtures.product("p1", "b1", "Coffee", 5);
            var tea = Fixtures.product("p2", "b1", "Tea", 7);
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(branches.findByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.just(downtown, uptown));
            when(products.findByBranchId("b1")).thenReturn(Flux.just(coffee, tea));
            when(products.findByBranchId("b2")).thenReturn(Flux.empty());

            StepVerifier.create(useCase.get(FRANCHISE_ID))
                    .expectNext(new FranchiseOverview(Fixtures.franchise(), List.of(
                            new BranchOverview(downtown, List.of(coffee, tea)),
                            new BranchOverview(uptown, List.of()))))
                    .verifyComplete();
        }

        @Test
        void returnsAnEmptyTreeForAFranchiseWithoutBranches() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(branches.findByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.empty());

            StepVerifier.create(useCase.get(FRANCHISE_ID))
                    .expectNext(new FranchiseOverview(Fixtures.franchise(), List.of()))
                    .verifyComplete();
        }

        @Test
        void failsWhenTheFranchiseDoesNotExist() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.get(FRANCHISE_ID))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }
}
