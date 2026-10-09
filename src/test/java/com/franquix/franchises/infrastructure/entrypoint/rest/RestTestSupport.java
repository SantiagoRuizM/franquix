package com.franquix.franchises.infrastructure.entrypoint.rest;

import com.franquix.franchises.domain.port.in.BranchUseCase;
import com.franquix.franchises.domain.port.in.FranchiseUseCase;
import com.franquix.franchises.domain.port.in.ProductUseCase;
import com.franquix.franchises.infrastructure.entrypoint.rest.error.GlobalErrorHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.error.ProblemDetailWriter;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.BranchHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.FranchiseHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.ProductHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.router.FranchiseRouter;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidator;
import com.franquix.franchises.infrastructure.security.ApiTokenFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Web slice: real router, handlers, validation, error handler and token filter; use cases are
 * mocked. {@link #client} sends the API token, {@link #anonymous} does not.
 */
@WebFluxTest
@Import({
        FranchiseRouter.class,
        FranchiseHandler.class,
        BranchHandler.class,
        ProductHandler.class,
        RequestValidator.class,
        ProblemDetailWriter.class,
        GlobalErrorHandler.class,
        ApiTokenFilter.class
})
public abstract class RestTestSupport {

    /** The {@code local} profile default token. */
    protected static final String API_TOKEN = "local-dev-token";
    protected static final String FRANCHISE_ID = "6650f1c2a1b2c3d4e5f60718";
    protected static final String BRANCH_ID = "6650f1c2a1b2c3d4e5f60719";
    protected static final String PRODUCT_ID = "6650f1c2a1b2c3d4e5f6071a";
    protected static final String BASE = "/api/v1/franchises";

    @Autowired
    protected WebTestClient anonymous;

    protected WebTestClient client;

    @MockitoBean
    protected FranchiseUseCase franchises;
    @MockitoBean
    protected BranchUseCase branches;
    @MockitoBean
    protected ProductUseCase products;

    @BeforeEach
    void authenticate() {
        client = anonymous.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + API_TOKEN).build();
    }
}
