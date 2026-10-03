package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.PageQuery;
import com.franquix.franchises.domain.model.PageResult;
import reactor.core.publisher.Mono;

/** Lists franchises one page at a time, ordered by name. */
@FunctionalInterface
public interface ListFranchisesUseCase {

    Mono<PageResult<Franchise>> list(PageQuery query);
}
