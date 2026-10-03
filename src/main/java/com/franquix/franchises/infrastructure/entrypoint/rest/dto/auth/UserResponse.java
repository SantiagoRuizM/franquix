package com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth;

import com.franquix.franchises.domain.model.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** A user profile. The password hash is never exposed. */
public record UserResponse(
        @Schema(example = "6650f1c2a1b2c3d4e5f60720") String id,
        @Schema(example = "viewer@franquix.local") String email,
        @Schema(example = "Store Viewer") String name,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
