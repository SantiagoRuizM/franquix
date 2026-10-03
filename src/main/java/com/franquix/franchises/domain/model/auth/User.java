package com.franquix.franchises.domain.model.auth;

import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.model.DomainRules;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * An account allowed to call the API. Only the password hash is ever held, never the password.
 *
 * @param id           identifier, {@code null} until persisted
 * @param email        login, stored trimmed and lower-cased so it is unique regardless of case
 * @param name         display name
 * @param passwordHash hash produced by the password hasher
 * @param role         permissions
 * @param active       inactive users cannot log in or refresh their session
 */
public record User(
        String id,
        String email,
        String name,
        String passwordHash,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static final int EMAIL_MAX_LENGTH = 254;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public User {
        email = normalizeEmail(email);
        name = DomainRules.requireValidName("name", name);
        passwordHash = DomainRules.requireReference("passwordHash", passwordHash);
        if (role == null) {
            throw new DomainValidationException("role is required");
        }
    }

    public static User create(String email, String name, String passwordHash, Role role, Instant now) {
        return new User(null, email, name, passwordHash, role, true, now, now);
    }

    /**
     * @return the email trimmed and lower-cased
     * @throws DomainValidationException when it is blank, too long or not shaped like an email
     */
    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainValidationException("email must not be blank");
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > EMAIL_MAX_LENGTH || !EMAIL.matcher(normalized).matches()) {
            throw new DomainValidationException("email must be a valid email address");
        }
        return normalized;
    }
}
