package com.franquix.franchises.domain.model;

import com.franquix.franchises.domain.exception.DomainValidationException;
import java.text.Normalizer;
import java.util.regex.Pattern;

/** Invariants shared by every aggregate. */
public final class DomainRules {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int STOCK_MIN = 0;

    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private DomainRules() {
    }

    /**
     * Normalizes and validates a name: Unicode NFC, trimmed, inner whitespace collapsed to one space.
     *
     * @return the normalized name
     * @throws DomainValidationException when the name is null, blank, too long, contains invisible or
     *     control characters, or has no letter or digit
     */
    public static String requireValidName(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("%s must not be blank".formatted(field));
        }
        String normalized = WHITESPACE_RUN.matcher(Normalizer.normalize(value, Normalizer.Form.NFC).strip())
                .replaceAll(" ");
        if (normalized.codePoints().anyMatch(DomainRules::isInvisible)) {
            throw new DomainValidationException(
                    "%s must not contain invisible or control characters".formatted(field));
        }
        if (normalized.codePoints().noneMatch(Character::isLetterOrDigit)) {
            throw new DomainValidationException("%s must contain at least one letter or digit".formatted(field));
        }
        if (normalized.length() > NAME_MAX_LENGTH) {
            throw new DomainValidationException(
                    "%s must be at most %d characters".formatted(field, NAME_MAX_LENGTH));
        }
        return normalized;
    }

    private static boolean isInvisible(int codePoint) {
        return switch (Character.getType(codePoint)) {
            case Character.CONTROL, Character.FORMAT, Character.PRIVATE_USE,
                 Character.SURROGATE, Character.UNASSIGNED -> true;
            default -> false;
        };
    }

    /**
     * @return the stock, unchanged
     * @throws DomainValidationException when the stock is negative
     */
    public static int requireValidStock(int stock) {
        if (stock < STOCK_MIN) {
            throw new DomainValidationException(
                    "stock must be greater than or equal to %d".formatted(STOCK_MIN));
        }
        return stock;
    }

    /**
     * @return the reference, unchanged
     * @throws DomainValidationException when a mandatory reference is missing
     */
    public static String requireReference(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("%s is required".formatted(field));
        }
        return value;
    }
}
