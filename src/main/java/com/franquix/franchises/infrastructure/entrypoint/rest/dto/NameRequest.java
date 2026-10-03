package com.franquix.franchises.infrastructure.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Body used to create or rename a franchise or a branch, and to rename a product. */
@Schema(description = "A name. It is trimmed; it must be 1-100 characters long and not blank.")
public record NameRequest(
        @Schema(example = "Downtown", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String name) {
}
