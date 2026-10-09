package com.franquix.franchises.application.usecase;

import static com.franquix.franchises.application.usecase.Fixtures.FRANCHISE_ID;
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

    @Nested
    class CreateFranchise {

        private FranchiseService useCase;

        @BeforeEach
        void setUp() {
            useCase = new FranchiseService(franchises, branches, products);
        }

        @Test
        void savesATrimmedFranchise() {
            Franchise expectedToSave = new Franchise(null, "Acme");
            Franchise saved = new Franchise(FRANCHISE_ID, "Acme");
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

        private FranchiseService useCase;

        @BeforeEach
        void setUp() {
            useCase = new FranchiseService(franchises, branches, products);
        }

        @Test
        void renamesAtomicallyWithTheTrimmedName() {
            Franchise renamed = new Franchise(FRANCHISE_ID, "Acme Global");
            when(franchises.rename(FRANCHISE_ID, "Acme Global")).thenReturn(Mono.just(renamed));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, "  Acme Global "))
                    .expectNext(renamed)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheFranchiseDoesNotExist() {
            when(franchises.rename(FRANCHISE_ID, "Acme Global")).thenReturn(Mono.empty());

            StepVerifier.create(useCase.rename(FRANCHISE_ID, "Acme Global"))
                    .expectErrorMessage("Franchise 'franchise-1' not found")
                    .verify();
        }

        @Test
        void propagatesDuplicateNameErrors() {
            when(franchises.rename(FRANCHISE_ID, "Other"))
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
            verify(franchises, never()).rename(any(), any());
        }
    }
}
