package com.franquix.franchises.architecture;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/** Proves BlockHound is active, so the rest of the suite really detects blocking calls. */
class BlockHoundInstalledTest {

    @Test
    void blockingOnAReactiveThreadFails() {
        Mono<Long> blocking = Mono.delay(Duration.ofMillis(1))
                .doOnNext(tick -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .subscribeOn(Schedulers.parallel());

        StepVerifier.create(blocking)
                .expectError(BlockingOperationError.class)
                .verify();
    }
}
