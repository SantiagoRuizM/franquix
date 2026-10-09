package com.franquix.franchises.infrastructure.entrypoint.rest.handler;

import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.in.ProductUseCase;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.CreateProductRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.NameRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.StockUpdateRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.mapper.RestMapper;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ProductHandler {

    private final ProductUseCase products;
    private final RequestValidator validator;

    public ProductHandler(ProductUseCase products, RequestValidator validator) {
        this.products = products;
        this.validator = validator;
    }

    public Mono<ServerResponse> add(ServerRequest request) {
        return Mono.defer(() -> {
            String franchiseId = validator.objectId(request, PathVariables.FRANCHISE_ID);
            String branchId = validator.objectId(request, PathVariables.BRANCH_ID);
            return validator.body(request, CreateProductRequest.class)
                    .flatMap(body -> products.add(franchiseId, branchId, body.name(), body.stock()));
        }).flatMap(product -> ServerResponse
                .created(request.uriBuilder().path("/{id}").build(product.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(RestMapper.toResponse(product)));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        return Mono.defer(() -> products.delete(
                        validator.objectId(request, PathVariables.FRANCHISE_ID),
                        validator.objectId(request, PathVariables.BRANCH_ID),
                        validator.objectId(request, PathVariables.PRODUCT_ID)))
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> updateStock(ServerRequest request) {
        return Mono.defer(() -> {
            ProductPath path = ProductPath.of(request, validator);
            return validator.body(request, StockUpdateRequest.class)
                    .flatMap(body -> products.updateStock(
                            path.franchiseId(), path.branchId(), path.productId(), body.stock()));
        }).flatMap(this::ok);
    }

    public Mono<ServerResponse> rename(ServerRequest request) {
        return Mono.defer(() -> {
            ProductPath path = ProductPath.of(request, validator);
            return validator.body(request, NameRequest.class)
                    .flatMap(body -> products.rename(
                            path.franchiseId(), path.branchId(), path.productId(), body.name()));
        }).flatMap(this::ok);
    }

    private Mono<ServerResponse> ok(Product product) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(RestMapper.toResponse(product));
    }

    private record ProductPath(String franchiseId, String branchId, String productId) {

        static ProductPath of(ServerRequest request, RequestValidator validator) {
            return new ProductPath(
                    validator.objectId(request, PathVariables.FRANCHISE_ID),
                    validator.objectId(request, PathVariables.BRANCH_ID),
                    validator.objectId(request, PathVariables.PRODUCT_ID));
        }
    }
}
