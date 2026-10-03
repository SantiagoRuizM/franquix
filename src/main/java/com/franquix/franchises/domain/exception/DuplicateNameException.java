package com.franquix.franchises.domain.exception;

/** A name is already taken within its uniqueness scope. */
public final class DuplicateNameException extends BusinessException {

    private DuplicateNameException(String message) {
        super(message);
    }

    public static DuplicateNameException franchise(String name) {
        return new DuplicateNameException("A franchise named '%s' already exists".formatted(name));
    }

    public static DuplicateNameException branch(String name) {
        return new DuplicateNameException(
                "A branch named '%s' already exists in this franchise".formatted(name));
    }

    public static DuplicateNameException product(String name) {
        return new DuplicateNameException(
                "A product named '%s' already exists in this branch".formatted(name));
    }
}
