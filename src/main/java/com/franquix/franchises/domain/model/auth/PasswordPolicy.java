package com.franquix.franchises.domain.model.auth;

import com.franquix.franchises.domain.exception.DomainValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Password rules, equivalent to Django's default validators: minimum length, not a common password,
 * not only digits, and not similar to the user's email.
 *
 * <p>The upper bound is measured in UTF-8 bytes because BCrypt only reads the first 72 bytes.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_BYTES = 72;

    /** Frequent choices from public breach lists that would otherwise satisfy the length rule. */
    private static final Set<String> COMMON = Set.of(
            "123456789012", "password1234", "passwordpassword", "qwertyuiop12", "qwertyuiopasdf",
            "iloveyou1234", "administrator", "adminadmin12", "letmein12345", "welcome12345",
            "changeme1234", "abc123456789", "000000000000", "111111111111", "aaaaaaaaaaaa",
            "franquix1234", "football1234", "baseball1234", "superman1234", "trustno1trustno1",
            "1q2w3e4r5t6y", "zaq12wsxcde3", "monkey123456", "dragon123456", "sunshine1234");

    private PasswordPolicy() {
    }

    /** @throws DomainValidationException describing the first rule the password breaks */
    public static void validate(String password, String email) {
        if (password == null || password.isBlank()) {
            throw new DomainValidationException("password must not be blank");
        }
        if (password.codePointCount(0, password.length()) < MIN_LENGTH) {
            throw new DomainValidationException(
                    "password must be at least %d characters".formatted(MIN_LENGTH));
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new DomainValidationException("password must be at most %d bytes".formatted(MAX_BYTES));
        }
        if (password.chars().allMatch(Character::isDigit)) {
            throw new DomainValidationException("password must not be entirely numeric");
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower)) {
            throw new DomainValidationException("password is too common");
        }
        if (email != null) {
            String localPart = email.strip().toLowerCase(Locale.ROOT).split("@", 2)[0];
            if (localPart.length() >= 3 && lower.contains(localPart)) {
                throw new DomainValidationException("password is too similar to the email");
            }
        }
    }
}
