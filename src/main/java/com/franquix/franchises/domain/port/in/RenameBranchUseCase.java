package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Branch;
import reactor.core.publisher.Mono;

/** Changes the name of a branch that belongs to the given franchise. */
@FunctionalInterface
public interface RenameBranchUseCase {

    Mono<Branch> rename(String franchiseId, String branchId, String newName);
}
