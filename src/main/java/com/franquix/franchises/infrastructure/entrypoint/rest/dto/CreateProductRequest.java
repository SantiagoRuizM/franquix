package com.franquix.franchises.infrastructure.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body used to add a product to a branch. */
public record CreateProductRequest(
        @Schema(example = "Coffee", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String name,

        @Schema(example = "120", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        @PositiveOrZero(message = "must be greater than or equal to 0")
        Integer stock) {
}
