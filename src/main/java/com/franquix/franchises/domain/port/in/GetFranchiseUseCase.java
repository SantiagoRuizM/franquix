package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.FranchiseOverview;
import reactor.core.publisher.Mono;

/** Returns a franchise together with its branches and their products. */
@FunctionalInterface
public interface GetFranchiseUseCase {

    Mono<FranchiseOverview> get(String franchiseId);
}
