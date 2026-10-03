package com.franquix.franchises.infrastructure.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/**
 * OpenAPI metadata, reusable RFC 7807 error responses and the bearer security scheme, so the
 * Swagger UI "Authorize" button works with the token returned by login.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";
    private static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final Set<String> PUBLIC_PATHS = Set.of("/api/v1/auth/login", "/api/v1/auth/refresh");

    @Bean
    OpenAPI franquixOpenApi(@Value("${info.app.version:1.0.0}") String version) {
        Components components = new Components()
                .addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token from POST /api/v1/auth/login"));
        ModelConverters.getInstance().read(ProblemDetail.class).forEach(components::addSchemas);
        components
                .addResponses("BadRequest", problem("Invalid id, body or business value"))
                .addResponses("Unauthorized", problem("Missing, invalid or expired credentials or token"))
                .addResponses("Forbidden", problem("Authenticated, but the role does not allow it"))
                .addResponses("NotFound", problem("Franchise, branch or product not found"))
                .addResponses("Conflict", problem("Name or email already in use within its scope"))
                .addResponses("TooManyRequests", problem("Rate limit exceeded; see the Retry-After header"));

        return new OpenAPI()
                .info(new Info()
                        .title("Franquix API")
                        .version(version)
                        .description("Franchiser as a Service - manage franchises, their branches and "
                                + "the stock of the products each branch offers. Log in, then send "
                                + "`Authorization: Bearer <accessToken>`. VIEWER can read, ADMIN can do everything."))
                .components(components);
    }

    /**
     * Marks every API operation except login and refresh as bearer-protected, and documents the
     * 401, 403 and 429 answers the security layer can give to any of them.
     */
    @Bean
    OpenApiCustomizer bearerSecurityCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, item) -> {
            if (path.startsWith("/api/") && !PUBLIC_PATHS.contains(path)) {
                item.readOperations().forEach(operation -> {
                    operation.addSecurityItem(new SecurityRequirement().addList(BEARER));
                    operation.getResponses().computeIfAbsent("401", code -> reference("Unauthorized"));
                    operation.getResponses().computeIfAbsent("403", code -> reference("Forbidden"));
                    operation.getResponses().computeIfAbsent("429", code -> reference("TooManyRequests"));
                });
            }
        });
    }

    private static ApiResponse reference(String name) {
        return new ApiResponse().$ref("#/components/responses/" + name);
    }

    private static ApiResponse problem(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_JSON,
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }
}
