package com.franquix.franchises.domain.port.out;

import com.franquix.franchises.domain.model.Branch;
import java.time.Instant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Persistence port for branches. */
public interface BranchRepositoryPort {

    /**
     * Inserts a new branch.
     *
     * @return the stored branch; errors with
     *     {@link com.franquix.franchises.domain.exception.DuplicateNameException} when the name is
     *     already used inside the same franchise
     */
    Mono<Branch> create(Branch branch);

    /**
     * Atomically sets the name of a branch that belongs to the franchise, touching no other field.
     *
     * @return the updated branch, or empty when it does not exist in that franchise
     */
    Mono<Branch> rename(String branchId, String franchiseId, String name, Instant updatedAt);

    /** @return the branch only when it exists and belongs to the franchise, otherwise empty */
    Mono<Branch> findByIdAndFranchiseId(String branchId, String franchiseId);

    /** @return every branch of the franchise, ordered by name ascending */
    Flux<Branch> findByFranchiseId(String franchiseId);
}
