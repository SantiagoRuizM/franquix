package com.franquix.franchises.infrastructure.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** A franchise with its branches (sorted by name) and each branch's products (sorted by name). */
public record FranchiseDetailResponse(
        @Schema(example = "6650f1c2a1b2c3d4e5f60718") String id,
        @Schema(example = "Acme Coffee") String name,
        Instant createdAt,
        Instant updatedAt,
        List<BranchDetail> branches) {

    public record BranchDetail(
            @Schema(example = "6650f1c2a1b2c3d4e5f60719") String id,
            @Schema(example = "Downtown") String name,
            Instant createdAt,
            Instant updatedAt,
            List<ProductResponse> products) {
    }
}
