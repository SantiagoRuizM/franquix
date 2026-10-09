package com.franquix.franchises.infrastructure.entrypoint.rest.router;

import com.franquix.franchises.infrastructure.entrypoint.rest.dto.BranchResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.CreateProductRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.FranchiseResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.NameRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.ProductResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.StockUpdateRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.TopStockProductResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.BranchHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.FranchiseHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.ProductHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Functional routes of the API. The {@link RouterOperations} annotations only feed the OpenAPI
 * document (springdoc cannot introspect handler functions); routing is defined in code below.
 */
@Configuration(proxyBeanMethods = false)
public class FranchiseRouter {

    public static final String BASE_PATH = "/api/v1/franchises";

    private static final String FRANCHISE = "/{franchiseId}";
    private static final String BRANCHES = FRANCHISE + "/branches";
    private static final String BRANCH = BRANCHES + "/{branchId}";
    private static final String PRODUCTS = BRANCH + "/products";
    private static final String PRODUCT = PRODUCTS + "/{productId}";

    @Bean
    @RouterOperations({
            @RouterOperation(path = BASE_PATH, method = RequestMethod.POST,
                    beanClass = FranchiseHandler.class, beanMethod = "create",
                    operation = @Operation(operationId = "createFranchise", tags = "Franchises",
                            summary = "Create a franchise",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = NameRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Franchise created",
                                            content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "409", ref = "Conflict")})),
            @RouterOperation(path = BASE_PATH + FRANCHISE + "/name", method = RequestMethod.PATCH,
                    beanClass = FranchiseHandler.class, beanMethod = "rename",
                    operation = @Operation(operationId = "renameFranchise", tags = "Franchises",
                            summary = "Rename a franchise",
                            parameters = @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true),
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = NameRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Franchise renamed",
                                            content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "404", ref = "NotFound"),
                                    @ApiResponse(responseCode = "409", ref = "Conflict")})),
            @RouterOperation(path = BASE_PATH + BRANCHES + "/top-stock-products", method = RequestMethod.GET,
                    beanClass = FranchiseHandler.class, beanMethod = "topStockProducts",
                    operation = @Operation(operationId = "getTopStockProducts", tags = "Franchises",
                            summary = "Product with the most stock in each branch of a franchise",
                            description = "One entry per branch, ordered by branch name. Branches without "
                                    + "products are omitted. Ties are broken by product name (ascending).",
                            parameters = @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Top-stock product per branch",
                                            content = @Content(array = @ArraySchema(
                                                    schema = @Schema(implementation = TopStockProductResponse.class)))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "404", ref = "NotFound")})),
            @RouterOperation(path = BASE_PATH + BRANCHES, method = RequestMethod.POST,
                    beanClass = BranchHandler.class, beanMethod = "add",
                    operation = @Operation(operationId = "addBranch", tags = "Branches",
                            summary = "Add a branch to a franchise",
                            parameters = @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true),
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = NameRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Branch created",
                                            content = @Content(schema = @Schema(implementation = BranchResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "404", ref = "NotFound"),
                                    @ApiResponse(responseCode = "409", ref = "Conflict")})),
            @RouterOperation(path = BASE_PATH + BRANCH + "/name", method = RequestMethod.PATCH,
                    beanClass = BranchHandler.class, beanMethod = "rename",
                    operation = @Operation(operationId = "renameBranch", tags = "Branches",
                            summary = "Rename a branch",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true)},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = NameRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Branch renamed",
                                            content = @Content(schema = @Schema(implementation = BranchResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "404", ref = "NotFound"),
                                    @ApiResponse(responseCode = "409", ref = "Conflict")})),
            @RouterOperation(path = BASE_PATH + PRODUCTS, method = RequestMethod.POST,
                    beanClass = ProductHandler.class, beanMethod = "add",
                    operation = @Operation(operationId = "addProduct", tags = "Products",
                            summary = "Add a product to a branch",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true)},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = CreateProductRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Product created",
                                            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "404", ref = "NotFound"),
                                    @ApiResponse(responseCode = "409", ref = "Conflict")})),
            @RouterOperation(path = BASE_PATH + PRODUCT, method = RequestMethod.DELETE,
                    beanClass = ProductHandler.class, beanMethod = "delete",
                    operation = @Operation(operationId = "deleteProduct", tags = "Products",
                            summary = "Delete a product from a branch",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true),
                                    @Parameter(in = ParameterIn.PATH, name = "productId", required = true)},
                            responses = {
                                    @ApiResponse(responseCode = "204", description = "Product deleted"),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "404", ref = "NotFound")})),
            @RouterOperation(path = BASE_PATH + PRODUCT + "/stock", method = RequestMethod.PATCH,
                    beanClass = ProductHandler.class, beanMethod = "updateStock",
                    operation = @Operation(operationId = "updateProductStock", tags = "Products",
                            summary = "Set the stock of a product",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true),
                                    @Parameter(in = ParameterIn.PATH, name = "productId", required = true)},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = StockUpdateRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Stock updated",
                                            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "404", ref = "NotFound")})),
            @RouterOperation(path = BASE_PATH + PRODUCT + "/name", method = RequestMethod.PATCH,
                    beanClass = ProductHandler.class, beanMethod = "rename",
                    operation = @Operation(operationId = "renameProduct", tags = "Products",
                            summary = "Rename a product",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true),
                                    @Parameter(in = ParameterIn.PATH, name = "productId", required = true)},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = NameRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Product renamed",
                                            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "404", ref = "NotFound"),
                                    @ApiResponse(responseCode = "409", ref = "Conflict")}))
    })
    RouterFunction<ServerResponse> franchiseRoutes(
            FranchiseHandler franchises, BranchHandler branches, ProductHandler products) {
        return RouterFunctions.route()
                .path(BASE_PATH, api -> api
                        .POST("", franchises::create)
                        .PATCH(FRANCHISE + "/name", franchises::rename)
                        .GET(BRANCHES + "/top-stock-products", franchises::topStockProducts)
                        .POST(BRANCHES, branches::add)
                        .PATCH(BRANCH + "/name", branches::rename)
                        .POST(PRODUCTS, products::add)
                        .DELETE(PRODUCT, products::delete)
                        .PATCH(PRODUCT + "/stock", products::updateStock)
                        .PATCH(PRODUCT + "/name", products::rename))
                .build();
    }
}
