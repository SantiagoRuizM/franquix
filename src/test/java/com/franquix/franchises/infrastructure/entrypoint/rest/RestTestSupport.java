package com.franquix.franchises.infrastructure.entrypoint.rest;

import com.franquix.franchises.domain.port.in.AddBranchUseCase;
import com.franquix.franchises.domain.port.in.AddProductUseCase;
import com.franquix.franchises.domain.port.in.CreateFranchiseUseCase;
import com.franquix.franchises.domain.port.in.DeleteProductUseCase;
import com.franquix.franchises.domain.port.in.GetFranchiseUseCase;
import com.franquix.franchises.domain.port.in.GetTopStockProductsUseCase;
import com.franquix.franchises.domain.port.in.ListFranchisesUseCase;
import com.franquix.franchises.domain.port.in.RenameBranchUseCase;
import com.franquix.franchises.domain.port.in.RenameFranchiseUseCase;
import com.franquix.franchises.domain.port.in.RenameProductUseCase;
import com.franquix.franchises.domain.port.in.UpdateProductStockUseCase;
import com.franquix.franchises.infrastructure.config.JacksonConfig;
import com.franquix.franchises.infrastructure.entrypoint.rest.error.GlobalErrorHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.BranchHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.FranchiseHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.ProductHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.router.FranchiseRouter;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Web slice: real router, handlers, validation and error handler; use cases are mocked.
 */
@WebFluxTest
@Import({
        FranchiseRouter.class,
        FranchiseHandler.class,
        BranchHandler.class,
        ProductHandler.class,
        RequestValidator.class,
        GlobalErrorHandler.class,
        JacksonConfig.class,
        RestTestSupport.FixedClock.class
})
public abstract class RestTestSupport {

    protected static final Instant NOW = Instant.parse("2026-03-15T12:30:00Z");
    protected static final String FRANCHISE_ID = "6650f1c2a1b2c3d4e5f60718";
    protected static final String BRANCH_ID = "6650f1c2a1b2c3d4e5f60719";
    protected static final String PRODUCT_ID = "6650f1c2a1b2c3d4e5f6071a";
    protected static final String BASE = "/api/v1/franchises";

    @Autowired
    protected WebTestClient client;

    @MockitoBean
    protected CreateFranchiseUseCase createFranchise;
    @MockitoBean
    protected ListFranchisesUseCase listFranchises;
    @MockitoBean
    protected GetFranchiseUseCase getFranchise;
    @MockitoBean
    protected RenameFranchiseUseCase renameFranchise;
    @MockitoBean
    protected GetTopStockProductsUseCase getTopStockProducts;
    @MockitoBean
    protected AddBranchUseCase addBranch;
    @MockitoBean
    protected RenameBranchUseCase renameBranch;
    @MockitoBean
    protected AddProductUseCase addProduct;
    @MockitoBean
    protected DeleteProductUseCase deleteProduct;
    @MockitoBean
    protected UpdateProductStockUseCase updateStock;
    @MockitoBean
    protected RenameProductUseCase renameProduct;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
