package com.franquix.franchises.domain.model.auth;

import java.time.Instant;

/** A signed, short-lived bearer token. */
public record AccessToken(String value, Instant expiresAt) {
}
