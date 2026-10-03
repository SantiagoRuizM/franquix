package com.franquix.franchises.domain.exception;

/**
 * Base type for every rule violation raised by the domain. Infrastructure maps each subtype to an
 * HTTP status, so the domain never needs to know about transport concerns.
 */
public abstract sealed class BusinessException extends RuntimeException
        permits DomainValidationException, NotFoundException, DuplicateNameException,
        AuthenticationFailedException {

    protected BusinessException(String message) {
        super(message);
    }
}
