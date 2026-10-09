package com.franquix.franchises.infrastructure.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BranchResponse(
        @Schema(example = "6650f1c2a1b2c3d4e5f60719") String id,
        @Schema(example = "6650f1c2a1b2c3d4e5f60718") String franchiseId,
        @Schema(example = "Downtown") String name) {
}
