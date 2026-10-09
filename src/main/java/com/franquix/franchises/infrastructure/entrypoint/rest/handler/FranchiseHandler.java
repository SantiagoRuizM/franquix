package com.franquix.franchises.infrastructure.entrypoint.rest.handler;

import com.franquix.franchises.domain.port.in.FranchiseUseCase;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.NameRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.mapper.RestMapper;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class FranchiseHandler {

    private final FranchiseUseCase franchises;
    private final RequestValidator validator;

    public FranchiseHandler(FranchiseUseCase franchises, RequestValidator validator) {
        this.franchises = franchises;
        this.validator = validator;
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return validator.body(request, NameRequest.class)
                .flatMap(body -> franchises.create(body.name()))
                .flatMap(franchise -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(franchise.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(RestMapper.toResponse(franchise)));
    }

    public Mono<ServerResponse> rename(ServerRequest request) {
        return Mono.defer(() -> {
            String franchiseId = validator.objectId(request, PathVariables.FRANCHISE_ID);
            return validator.body(request, NameRequest.class)
                    .flatMap(body -> franchises.rename(franchiseId, body.name()));
        }).flatMap(franchise -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(RestMapper.toResponse(franchise)));
    }

    public Mono<ServerResponse> topStockProducts(ServerRequest request) {
        // collected (one entry per branch, so small) so a 404 is raised before the response is committed
        return Mono.defer(() -> franchises
                        .getTopStockProducts(validator.objectId(request, PathVariables.FRANCHISE_ID))
                        .map(RestMapper::toResponse)
                        .collectList())
                .flatMap(tops -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(tops));
    }
}
