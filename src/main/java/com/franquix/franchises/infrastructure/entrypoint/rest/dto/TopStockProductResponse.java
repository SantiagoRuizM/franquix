package com.franquix.franchises.infrastructure.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** The product with the most stock in a branch, and the branch it belongs to. */
public record TopStockProductResponse(
        @Schema(example = "6650f1c2a1b2c3d4e5f60719") String branchId,
        @Schema(example = "Downtown") String branchName,
        @Schema(example = "6650f1c2a1b2c3d4e5f6071a") String productId,
        @Schema(example = "Coffee") String productName,
        @Schema(example = "120") int stock) {
}
