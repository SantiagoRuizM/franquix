package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.port.in.AddBranchUseCase;
import com.franquix.franchises.domain.port.in.AddProductUseCase;
import com.franquix.franchises.domain.port.in.ChangePasswordUseCase;
import com.franquix.franchises.domain.port.in.CreateFranchiseUseCase;
import com.franquix.franchises.domain.port.in.DeleteProductUseCase;
import com.franquix.franchises.domain.port.in.GetCurrentUserUseCase;
import com.franquix.franchises.domain.port.in.GetFranchiseUseCase;
import com.franquix.franchises.domain.port.in.GetTopStockProductsUseCase;
import com.franquix.franchises.domain.port.in.ListFranchisesUseCase;
import com.franquix.franchises.domain.port.in.LoginUseCase;
import com.franquix.franchises.domain.port.in.LogoutUseCase;
import com.franquix.franchises.domain.port.in.RefreshSessionUseCase;
import com.franquix.franchises.domain.port.in.RegisterUserUseCase;
import com.franquix.franchises.domain.port.in.RenameBranchUseCase;
import com.franquix.franchises.domain.port.in.RenameFranchiseUseCase;
import com.franquix.franchises.domain.port.in.RenameProductUseCase;
import com.franquix.franchises.domain.port.in.UpdateProductStockUseCase;
import com.franquix.franchises.infrastructure.config.JacksonConfig;
import com.franquix.franchises.infrastructure.entrypoint.rest.error.GlobalErrorHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.error.ProblemDetailWriter;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.AuthHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.BranchHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.FranchiseHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.ProductHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.UserHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.router.AuthRouter;
import com.franquix.franchises.infrastructure.entrypoint.rest.router.FranchiseRouter;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidator;
import com.franquix.franchises.infrastructure.security.SecurityConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Web slice: real routers, handlers, validation, error handling and security (JWT resource server,
 * role rules, rate limiting); use cases are mocked. {@link #client} is authenticated as an ADMIN.
 */
@WebFluxTest
// generous budgets: the whole web slice shares one context; RateLimitTest lowers them on purpose
@TestPropertySource(properties = {
        "franquix.security.rate-limit.auth-per-minute=10000",
        "franquix.security.rate-limit.anonymous-per-hour=10000",
        "franquix.security.rate-limit.user-per-hour=10000"})
@Import({
        FranchiseRouter.class,
        AuthRouter.class,
        FranchiseHandler.class,
        BranchHandler.class,
        ProductHandler.class,
        AuthHandler.class,
        UserHandler.class,
        RequestValidator.class,
        ProblemDetailWriter.class,
        GlobalErrorHandler.class,
        JacksonConfig.class,
        SecurityConfig.class,
        RestTestSupport.FixedClock.class
})
public abstract class RestTestSupport {

    protected static final Instant NOW = Instant.parse("2026-03-15T12:30:00Z");
    protected static final String FRANCHISE_ID = "6650f1c2a1b2c3d4e5f60718";
    protected static final String BRANCH_ID = "6650f1c2a1b2c3d4e5f60719";
    protected static final String PRODUCT_ID = "6650f1c2a1b2c3d4e5f6071a";
    protected static final String USER_ID = "6650f1c2a1b2c3d4e5f60720";
    protected static final String BASE = "/api/v1/franchises";

    @Autowired
    protected WebTestClient anonymous;

    /** Authenticated as {@link Role#ADMIN}. */
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
    @MockitoBean
    protected LoginUseCase login;
    @MockitoBean
    protected RefreshSessionUseCase refreshSession;
    @MockitoBean
    protected LogoutUseCase logout;
    @MockitoBean
    protected ChangePasswordUseCase changePassword;
    @MockitoBean
    protected GetCurrentUserUseCase currentUser;
    @MockitoBean
    protected RegisterUserUseCase registerUser;

    @BeforeEach
    void authenticateAsAdmin() {
        client = as(Role.ADMIN);
    }

    /** A client carrying a valid JWT for a user with the given role. */
    protected WebTestClient as(Role role) {
        return anonymous.mutateWith(mockJwt()
                .jwt(jwt -> jwt.subject(USER_ID).claim("role", role.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
