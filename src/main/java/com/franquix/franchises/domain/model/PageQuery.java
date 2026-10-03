package com.franquix.franchises.domain.model;

import com.franquix.franchises.domain.exception.DomainValidationException;

/**
 * A zero-based page request with a bounded size, so a client can never ask for an unbounded result.
 *
 * @param page zero-based page number
 * @param size items per page, 1 to {@value #MAX_SIZE}
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new DomainValidationException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new DomainValidationException("size must be between 1 and %d".formatted(MAX_SIZE));
        }
    }

    /** Number of items to skip, computed as {@code long} so large page numbers cannot overflow. */
    public long offset() {
        return (long) page * size;
    }
}
