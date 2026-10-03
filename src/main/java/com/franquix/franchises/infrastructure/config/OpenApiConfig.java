package com.franquix.franchises.infrastructure.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/** OpenAPI metadata plus reusable RFC 7807 error responses referenced by the routes. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Bean
    OpenAPI franquixOpenApi(@Value("${info.app.version:1.0.0}") String version) {
        Components components = new Components();
        ModelConverters.getInstance().read(ProblemDetail.class).forEach(components::addSchemas);
        components
                .addResponses("BadRequest", problem("Invalid id, body or business value"))
                .addResponses("NotFound", problem("Franchise, branch or product not found"))
                .addResponses("Conflict", problem("Name already in use within its scope"));

        return new OpenAPI()
                .info(new Info()
                        .title("Franquix API")
                        .version(version)
                        .description("Franchiser as a Service - manage franchises, their branches and "
                                + "the stock of the products each branch offers."))
                .components(components);
    }

    private static ApiResponse problem(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_JSON,
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }
}
