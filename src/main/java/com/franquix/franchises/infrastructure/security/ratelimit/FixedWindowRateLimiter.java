package com.franquix.franchises.infrastructure.security.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory fixed-window counter per key (client IP or user).
 *
 * <p>State is local to the instance, which matches the deployment ({@code max_instances: 1}). With
 * several instances each one would enforce its own budget; a shared store (e.g. Redis) would then be
 * needed. Expired windows are purged once the map grows past {@link #PURGE_THRESHOLD} keys, so a
 * flood of distinct IPs cannot grow memory without bound.
 */
public class FixedWindowRateLimiter {

    static final int PURGE_THRESHOLD = 10_000;

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public FixedWindowRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public Decision tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        if (windows.size() > PURGE_THRESHOLD) {
            windows.values().removeIf(existing -> existing.resetAtMillis() <= now);
        }
        Window current = windows.compute(key, (k, existing) -> existing == null || now >= existing.resetAtMillis()
                ? new Window(now + window.toMillis(), 1)
                : new Window(existing.resetAtMillis(), existing.count() + 1));
        if (current.count() <= limit) {
            return Decision.ALLOWED;
        }
        long retryAfterSeconds = Math.max(1, (current.resetAtMillis() - now + 999) / 1000);
        return new Decision(false, retryAfterSeconds);
    }

    int trackedKeys() {
        return windows.size();
    }

    /** @param retryAfterSeconds seconds until the window resets; meaningless when allowed */
    public record Decision(boolean allowed, long retryAfterSeconds) {

        static final Decision ALLOWED = new Decision(true, 0);
    }

    private record Window(long resetAtMillis, int count) {
    }
}
