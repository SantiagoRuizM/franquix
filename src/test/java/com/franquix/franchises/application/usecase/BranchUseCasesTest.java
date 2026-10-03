package com.franquix.franchises.application.usecase;

import static com.franquix.franchises.application.usecase.Fixtures.BRANCH_ID;
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
import com.franquix.franchises.domain.model.Branch;
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
class BranchUseCasesTest {

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
    class AddBranch {

        private AddBranchService useCase;

        @BeforeEach
        void setUp() {
            useCase = new AddBranchService(hierarchy, branches, CLOCK);
        }

        @Test
        void savesANewBranchUnderTheFranchise() {
            Branch toSave = new Branch(null, FRANCHISE_ID, "Downtown", NOW, NOW);
            Branch saved = new Branch(BRANCH_ID, FRANCHISE_ID, "Downtown", NOW, NOW);
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(branches.save(toSave)).thenReturn(Mono.just(saved));

            StepVerifier.create(useCase.add(FRANCHISE_ID, " Downtown "))
                    .expectNext(saved)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheFranchiseDoesNotExist() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.add(FRANCHISE_ID, "Downtown"))
                    .expectError(NotFoundException.class)
                    .verify();
            verify(branches, never()).save(any());
        }

        @Test
        void propagatesDuplicateNameErrors() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(branches.save(any())).thenReturn(Mono.error(DuplicateNameException.branch("Downtown")));

            StepVerifier.create(useCase.add(FRANCHISE_ID, "Downtown"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }

        @Test
        void rejectsAnInvalidName() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));

            StepVerifier.create(useCase.add(FRANCHISE_ID, "x".repeat(101)))
                    .expectError(DomainValidationException.class)
                    .verify();
        }
    }

    @Nested
    class RenameBranch {

        private RenameBranchService useCase;

        @BeforeEach
        void setUp() {
            useCase = new RenameBranchService(hierarchy, branches, CLOCK);
        }

        @Test
        void savesTheRenamedBranch() {
            Branch renamed = new Branch(BRANCH_ID, FRANCHISE_ID, "Uptown", CREATED, NOW);
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.branch()));
            when(branches.save(renamed)).thenReturn(Mono.just(renamed));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, "Uptown"))
                    .expectNext(renamed)
                    .verifyComplete();
        }

        @Test
        void failsWhenTheBranchIsNotInTheFranchise() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, "Uptown"))
                    .expectError(NotFoundException.class)
                    .verify();
            verify(branches, never()).save(any());
        }

        @Test
        void propagatesDuplicateNameErrors() {
            when(franchises.findById(FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.franchise()));
            when(branches.findByIdAndFranchiseId(BRANCH_ID, FRANCHISE_ID)).thenReturn(Mono.just(Fixtures.branch()));
            when(branches.save(any())).thenReturn(Mono.error(DuplicateNameException.branch("Uptown")));

            StepVerifier.create(useCase.rename(FRANCHISE_ID, BRANCH_ID, "Uptown"))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }
    }
}
