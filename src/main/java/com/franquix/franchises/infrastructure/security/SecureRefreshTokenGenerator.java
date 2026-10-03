package com.franquix.franchises.infrastructure.security;

import com.franquix.franchises.domain.port.out.RefreshTokenGeneratorPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 256-bit random tokens, URL-safe Base64 encoded. Only their SHA-256 is stored: a token has full
 * entropy, so a fast hash is enough (no salt or slow hash needed, unlike passwords).
 */
@Component
public class SecureRefreshTokenGenerator implements RefreshTokenGeneratorPort {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public Mono<String> generate() {
        // SecureRandom may read the OS entropy source: keep it off event-loop threads
        return Mono.fromCallable(() -> {
            byte[] bytes = new byte[TOKEN_BYTES];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }
}
