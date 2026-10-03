package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.PageQuery;
import com.franquix.franchises.domain.model.PageResult;
import com.franquix.franchises.domain.port.in.ListFranchisesUseCase;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import reactor.core.publisher.Mono;

public class ListFranchisesService implements ListFranchisesUseCase {

    private final FranchiseRepositoryPort franchises;

    public ListFranchisesService(FranchiseRepositoryPort franchises) {
        this.franchises = franchises;
    }

    @Override
    public Mono<PageResult<Franchise>> list(PageQuery query) {
        return Mono.zip(franchises.findPage(query).collectList(), franchises.count())
                .map(pageAndTotal -> new PageResult<>(
                        pageAndTotal.getT1(), query.page(), query.size(), pageAndTotal.getT2()));
    }
}
