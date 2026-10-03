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
                // The MongoDB driver guards short in-memory critical sections (cluster clock, connection
                // pool bookkeeping) with its own lock helper; under contention a thread parks for a few
                // microseconds. No I/O happens inside. Found by the concurrent tests (refresh-token
                // consume, parallel creates), which hung or failed before this allowance.
                .allowBlockingCallsInside("com.mongodb.internal.Locks", "withLock")
                .allowBlockingCallsInside("com.mongodb.internal.Locks", "withInterruptibleLock")
                .allowBlockingCallsInside("com.mongodb.internal.Locks", "checkedWithLock")
                .allowBlockingCallsInside("com.mongodb.internal.Locks", "checkedWithInterruptibleLock")
                .allowBlockingCallsInside("com.mongodb.internal.Locks", "lockInterruptibly")
                // print the offending stack: errors raised inside driver callbacks can otherwise be swallowed
                .blockingMethodCallback(method -> {
                    BlockingOperationError error = new BlockingOperationError(method);
                    error.printStackTrace();
                    throw error;
                });
    }
}
