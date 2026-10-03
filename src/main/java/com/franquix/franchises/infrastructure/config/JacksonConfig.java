package com.franquix.franchises.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Strict request contract: a value must arrive with the JSON type the API documents.
 *
 * <p>Jackson is lenient by default. It truncates {@code 1.9} to {@code 1}, turns {@code "5"} into a number
 * and {@code 123} into a name, and ignores unknown fields such as a misspelled {@code "stok"}. All of
 * those now fail with 400.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictRequestContract() {
        return builder -> builder
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .postConfigurer(mapper -> {
                    // numbers must be JSON numbers, not strings
                    mapper.coercionConfigFor(LogicalType.Integer)
                            .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
                    // text must be a JSON string, not a number or boolean
                    mapper.coercionConfigFor(LogicalType.Textual)
                            .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
                });
    }
}
