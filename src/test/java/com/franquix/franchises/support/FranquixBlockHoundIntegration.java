package com.franquix.franchises.support;

import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.blockhound.integration.BlockHoundIntegration;

/**
 * BlockHound configuration for the test suite (registered through {@code META-INF/services}).
 *
 * <p>Only third-party code that is known to block outside the request path is allowed; application
 * code gets no exemption.
 */
public class FranquixBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder
                // springdoc scans the classpath once, lazily, when /v3/api-docs is first requested
                .allowBlockingCallsInside("org.springdoc.api.AbstractOpenApiResource", "getOpenApi")
                // print the offending stack: errors raised inside driver callbacks can otherwise be swallowed
                .blockingMethodCallback(method -> {
                    BlockingOperationError error = new BlockingOperationError(method);
                    error.printStackTrace();
                    throw error;
                });
    }
}
