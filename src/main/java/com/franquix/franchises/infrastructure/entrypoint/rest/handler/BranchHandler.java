package com.franquix.franchises.infrastructure.entrypoint.rest.handler;

import com.franquix.franchises.domain.port.in.AddBranchUseCase;
import com.franquix.franchises.domain.port.in.RenameBranchUseCase;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.NameRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.mapper.RestMapper;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class BranchHandler {

    private final AddBranchUseCase addBranch;
    private final RenameBranchUseCase renameBranch;
    private final RequestValidator validator;

    public BranchHandler(AddBranchUseCase addBranch, RenameBranchUseCase renameBranch, RequestValidator validator) {
        this.addBranch = addBranch;
        this.renameBranch = renameBranch;
        this.validator = validator;
    }

    public Mono<ServerResponse> add(ServerRequest request) {
        return Mono.defer(() -> {
            String franchiseId = validator.objectId(request, PathVariables.FRANCHISE_ID);
            return validator.body(request, NameRequest.class)
                    .flatMap(body -> addBranch.add(franchiseId, body.name()));
        }).flatMap(branch -> ServerResponse
                .created(request.uriBuilder().path("/{id}").build(branch.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(RestMapper.toResponse(branch)));
    }

    public Mono<ServerResponse> rename(ServerRequest request) {
        return Mono.defer(() -> {
            String franchiseId = validator.objectId(request, PathVariables.FRANCHISE_ID);
            String branchId = validator.objectId(request, PathVariables.BRANCH_ID);
            return validator.body(request, NameRequest.class)
                    .flatMap(body -> renameBranch.rename(franchiseId, branchId, body.name()));
        }).flatMap(branch -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(RestMapper.toResponse(branch)));
    }
}
