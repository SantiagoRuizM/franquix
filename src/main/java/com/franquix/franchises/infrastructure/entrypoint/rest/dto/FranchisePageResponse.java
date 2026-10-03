package com.franquix.franchises.infrastructure.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** A page of franchises, ordered by name. */
public record FranchisePageResponse(
        List<FranchiseResponse> items,
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "42") long totalElements,
        @Schema(example = "3") long totalPages) {
}
