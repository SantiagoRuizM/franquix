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
import com.franquix.franchises.domain.model.Franchise;
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
        hierarchy = new HierarchyResolver(franchises, branches);
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
            when(franchises.create(expectedToSave)).thenReturn(Mono.just(saved));

            StepVerifier.create(useCase.create("  Acme "))
                    .expectNext(saved)
                    .verifyComplete();
        }

        @Test
        void propagatesDuplicateNameErrors() {
            when(franchises.create(any())).thenReturn(Mono.error(DuplicateNameException.franchise("Acme")));

            StepVerifier.create(useCase.create("Acme"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }

        @Test
        void failsWithoutTouchingTheRepositoryWhenTheNameIsInvalid() {
            StepVerifier.create(useCase.create("  "))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(franchises, never()).create(any());
        }
    }

    @Nested
    class RenameFranchise {

        private RenameFranchiseService useCase;

        @BeforeEach
        void setUp() {
            useCase = new RenameFranchiseService(franchises, CLOCK);
        }

        @Test
        void renamesAtomicallyWithTheTrimmedName() {
            Franchise renamed = new Franchise(FRANCHISE_ID, "Acme Global", CREATED, NOW);
            when(franchises.rename(FRANCHISE_ID, "Acme Global", NOW)).thenReturn(Mono.just(renamed));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, "  Acme Global "))
                    .expectNext(renamed)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheFranchiseDoesNotExist() {
            when(franchises.rename(FRANCHISE_ID, "Acme Global", NOW)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.rename(FRANCHISE_ID, "Acme Global"))
                    .expectErrorMessage("Franchise 'franchise-1' not found")
                    .verify();
        }

        @Test
        void propagatesDuplicateNameErrors() {
            when(franchises.rename(FRANCHISE_ID, "Other", NOW))
                    .thenReturn(Mono.error(DuplicateNameException.franchise("Other")));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, "Other"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }

        @Test
        void rejectsAnInvalidNameWithoutTouchingTheRepository() {
            StepVerifier.create(useCase.rename(FRANCHISE_ID, ""))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(franchises, never()).rename(any(), any(), any());
        }
    }
}
