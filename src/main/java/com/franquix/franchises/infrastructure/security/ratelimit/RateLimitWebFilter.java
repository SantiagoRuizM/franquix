package com.franquix.franchises.infrastructure.security.ratelimit;

import com.franquix.franchises.infrastructure.entrypoint.rest.error.ProblemDetailWriter;
import com.franquix.franchises.infrastructure.security.SecurityProperties;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Request budgets, mirroring the Logistics throttles:
 *
 * <ul>
 *   <li>login, refresh and password change: {@code authPerMinute} per client IP</li>
 *   <li>authenticated requests: {@code userPerHour} per user</li>
 *   <li>other anonymous requests: {@code anonymousPerHour} per client IP</li>
 * </ul>
 *
 * <p>Health checks and API documentation are not counted. Runs right after authentication, so the
 * user is known. Behind the Cloudflare Worker the client IP comes from {@code X-Forwarded-For}, which
 * the Worker overwrites with {@code CF-Connecting-IP} and the {@code prod} profile trusts.
 */
public class RateLimitWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitWebFilter.class);
    private static final Set<String> AUTH_PATHS = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/password");
    private static final Set<String> UNMETERED_PREFIXES = Set.of(
            "/actuator/", "/v3/api-docs", "/swagger-ui", "/webjars/");

    private final FixedWindowRateLimiter limiter;
    private final SecurityProperties.RateLimit limits;
    private final ProblemDetailWriter writer;

    public RateLimitWebFilter(
            FixedWindowRateLimiter limiter, SecurityProperties.RateLimit limits, ProblemDetailWriter writer) {
        this.limiter = limiter;
        this.limits = limits;
        this.writer = writer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (UNMETERED_PREFIXES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        String ip = clientIp(exchange);
        if (AUTH_PATHS.contains(path)) {
            var decision = limiter.tryAcquire("auth:" + ip, limits.authPerMinute(), Duration.ofMinutes(1));
            if (!decision.allowed()) {
                return reject(exchange, decision, "auth");
            }
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(authentication -> limiter.tryAcquire(
                        "user:" + authentication.getName(), limits.userPerHour(), Duration.ofHours(1)))
                .switchIfEmpty(Mono.fromSupplier(() -> limiter.tryAcquire(
                        "anon:" + ip, limits.anonymousPerHour(), Duration.ofHours(1))))
                .flatMap(decision -> decision.allowed() ? chain.filter(exchange) : reject(exchange, decision, "api"));
    }

    private Mono<Void> reject(ServerWebExchange exchange, FixedWindowRateLimiter.Decision decision, String budget) {
        log.warn("status=429 budget={} path={} retryAfter={}s",
                budget, exchange.getRequest().getPath().value(), decision.retryAfterSeconds());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded, retry in %d seconds".formatted(decision.retryAfterSeconds()));
        return writer.write(exchange, problem, headers);
    }

    private static String clientIp(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(address -> address.getHostAddress())
                .orElse("unknown");
    }
}
