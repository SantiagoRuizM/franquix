package com.franquix.franchises.infrastructure.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record ProductResponse(
        @Schema(example = "6650f1c2a1b2c3d4e5f6071a") String id,
        @Schema(example = "6650f1c2a1b2c3d4e5f60719") String branchId,
        @Schema(example = "Coffee") String name,
        @Schema(example = "120") int stock,
        Instant createdAt,
        Instant updatedAt) {
}
