package com.franquix.franchises.infrastructure.entrypoint.rest.handler;

import com.franquix.franchises.domain.port.in.CreateFranchiseUseCase;
import com.franquix.franchises.domain.port.in.GetFranchiseUseCase;
import com.franquix.franchises.domain.port.in.GetTopStockProductsUseCase;
import com.franquix.franchises.domain.port.in.RenameFranchiseUseCase;
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

    private final CreateFranchiseUseCase createFranchise;
    private final GetFranchiseUseCase getFranchise;
    private final RenameFranchiseUseCase renameFranchise;
    private final GetTopStockProductsUseCase getTopStockProducts;
    private final RequestValidator validator;

    public FranchiseHandler(
            CreateFranchiseUseCase createFranchise,
            GetFranchiseUseCase getFranchise,
            RenameFranchiseUseCase renameFranchise,
            GetTopStockProductsUseCase getTopStockProducts,
            RequestValidator validator) {
        this.createFranchise = createFranchise;
        this.getFranchise = getFranchise;
        this.renameFranchise = renameFranchise;
        this.getTopStockProducts = getTopStockProducts;
        this.validator = validator;
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return validator.body(request, NameRequest.class)
                .flatMap(body -> createFranchise.create(body.name()))
                .flatMap(franchise -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(franchise.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(RestMapper.toResponse(franchise)));
    }

    public Mono<ServerResponse> get(ServerRequest request) {
        return Mono.defer(() -> getFranchise.get(validator.objectId(request, PathVariables.FRANCHISE_ID)))
                .flatMap(overview -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(RestMapper.toResponse(overview)));
    }

    public Mono<ServerResponse> rename(ServerRequest request) {
        return Mono.defer(() -> {
            String franchiseId = validator.objectId(request, PathVariables.FRANCHISE_ID);
            return validator.body(request, NameRequest.class)
                    .flatMap(body -> renameFranchise.rename(franchiseId, body.name()));
        }).flatMap(franchise -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(RestMapper.toResponse(franchise)));
    }

    public Mono<ServerResponse> topStockProducts(ServerRequest request) {
        // collected (one entry per branch, so small) so a 404 is raised before the response is committed
        return Mono.defer(() -> getTopStockProducts
                        .getTopStockProducts(validator.objectId(request, PathVariables.FRANCHISE_ID))
                        .map(RestMapper::toResponse)
                        .collectList())
                .flatMap(tops -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(tops));
    }
}
