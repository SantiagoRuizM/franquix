package com.franquix.franchises.infrastructure.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body used to set the stock of a product. */
public record StockUpdateRequest(
        @Schema(example = "85", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        @PositiveOrZero(message = "must be greater than or equal to 0")
        Integer stock) {
}
