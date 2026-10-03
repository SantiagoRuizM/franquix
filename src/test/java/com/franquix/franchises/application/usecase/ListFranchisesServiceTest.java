package com.franquix.franchises.application.usecase;

import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.model.PageQuery;
import com.franquix.franchises.domain.model.PageResult;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ListFranchisesServiceTest {

    @Mock
    private FranchiseRepositoryPort franchises;

    @Test
    void combinesThePageWithTheTotal() {
        PageQuery query = new PageQuery(1, 2);
        when(franchises.findPage(query)).thenReturn(Flux.just(Fixtures.franchise()));
        when(franchises.count()).thenReturn(Mono.just(3L));

        StepVerifier.create(new ListFranchisesService(franchises).list(query))
                .expectNext(new PageResult<>(List.of(Fixtures.franchise()), 1, 2, 3))
                .verifyComplete();
    }

    @Test
    void returnsAnEmptyPagePastTheEnd() {
        PageQuery query = new PageQuery(9, 20);
        when(franchises.findPage(query)).thenReturn(Flux.empty());
        when(franchises.count()).thenReturn(Mono.just(3L));

        StepVerifier.create(new ListFranchisesService(franchises).list(query))
                .expectNext(new PageResult<>(List.of(), 9, 20, 3))
                .verifyComplete();
    }
}
