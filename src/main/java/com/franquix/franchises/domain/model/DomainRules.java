package com.franquix.franchises.domain.model;

import com.franquix.franchises.domain.exception.DomainValidationException;

/** Invariants shared by every aggregate. */
public final class DomainRules {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int STOCK_MIN = 0;

    private DomainRules() {
    }

    /**
     * Trims and validates a name.
     *
     * @return the trimmed name
     * @throws DomainValidationException when the name is null, blank or longer than 100 characters
     */
    public static String requireValidName(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("%s must not be blank".formatted(field));
        }
        String trimmed = value.strip();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new DomainValidationException(
                    "%s must be at most %d characters".formatted(field, NAME_MAX_LENGTH));
        }
        return trimmed;
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
