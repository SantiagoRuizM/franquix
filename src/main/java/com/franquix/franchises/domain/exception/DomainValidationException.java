package com.franquix.franchises.domain.exception;

/** A value does not satisfy a domain invariant (blank name, negative stock, ...). */
public final class DomainValidationException extends BusinessException {

    public DomainValidationException(String message) {
        super(message);
    }
}
