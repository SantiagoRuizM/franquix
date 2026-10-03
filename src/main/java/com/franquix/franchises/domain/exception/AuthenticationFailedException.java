package com.franquix.franchises.domain.exception;

/**
 * Credentials or a refresh token were rejected. The messages are deliberately generic, so a caller
 * cannot tell an unknown email from a wrong password or an inactive account.
 */
public final class AuthenticationFailedException extends BusinessException {

    private AuthenticationFailedException(String message) {
        super(message);
    }

    public static AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("Invalid email or password");
    }

    public static AuthenticationFailedException invalidRefreshToken() {
        return new AuthenticationFailedException("Invalid or expired refresh token");
    }
}
