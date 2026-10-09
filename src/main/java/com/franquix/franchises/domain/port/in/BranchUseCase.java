package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Branch;
import reactor.core.publisher.Mono;

/** Branch operations. Every one checks that the branch belongs to the given franchise. */
public interface BranchUseCase {

    /** Adds a new branch to an existing franchise. */
    Mono<Branch> add(String franchiseId, String name);

    /** Changes the name of a branch of the franchise. */
    Mono<Branch> rename(String franchiseId, String branchId, String newName);
}
