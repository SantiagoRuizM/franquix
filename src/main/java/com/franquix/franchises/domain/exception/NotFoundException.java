package com.franquix.franchises.domain.exception;

/** The requested resource does not exist, or does not belong to the given parent. */
public final class NotFoundException extends BusinessException {

    private NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException franchise(String franchiseId) {
        return new NotFoundException("Franchise '%s' not found".formatted(franchiseId));
    }

    public static NotFoundException branch(String franchiseId, String branchId) {
        return new NotFoundException(
                "Branch '%s' not found in franchise '%s'".formatted(branchId, franchiseId));
    }

    public static NotFoundException product(String branchId, String productId) {
        return new NotFoundException(
                "Product '%s' not found in branch '%s'".formatted(productId, branchId));
    }
}
