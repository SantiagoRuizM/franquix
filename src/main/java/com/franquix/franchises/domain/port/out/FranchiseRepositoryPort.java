package com.franquix.franchises.domain.port.out;

import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.PageQuery;
import java.time.Instant;
import reactor.core.publisher.Flux;
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
    Mono<Franchise> rename(String franchiseId, String name, Instant updatedAt);

    /** @return the franchise, or an empty {@link Mono} when it does not exist */
    Mono<Franchise> findById(String franchiseId);

    /** @return the requested page of franchises, ordered by name then id */
    Flux<Franchise> findPage(PageQuery query);

    /** @return the total number of franchises */
    Mono<Long> count();
}
