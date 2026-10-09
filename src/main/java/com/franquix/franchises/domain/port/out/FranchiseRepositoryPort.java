package com.franquix.franchises.domain.port.out;

import com.franquix.franchises.domain.model.Franchise;
import reactor.core.publisher.Mono;

/** Persistence port for franchises. */
public interface FranchiseRepositoryPort {

    /**
     * Inserts a new franchise.
     *
     * @return the stored franchise; errors with
     *     {@link com.franquix.franchises.domain.exception.DuplicateNameException} when the name is taken
     */
    Mono<Franchise> create(Franchise franchise);

    /**
     * Atomically sets the name of an existing franchise, touching no other field.
     *
     * @return the updated franchise, or empty when it does not exist; errors with
     *     {@link com.franquix.franchises.domain.exception.DuplicateNameException} when the name is taken
     */
    Mono<Franchise> rename(String franchiseId, String name);

    /** @return the franchise, or an empty {@link Mono} when it does not exist */
    Mono<Franchise> findById(String franchiseId);
}
