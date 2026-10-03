package com.franquix.franchises.infrastructure.security;

import com.franquix.franchises.domain.model.auth.AccessToken;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.out.AccessTokenIssuerPort;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Issues HS256 JWTs: subject = user id, plus email, name and role claims. */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuerPort {

    static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Mono<AccessToken> issue(User user) {
        // UUID.randomUUID() may read the OS entropy source: keep it off event-loop threads
        return Mono.fromCallable(() -> encode(user)).subscribeOn(Schedulers.boundedElastic());
    }

    private AccessToken encode(User user) {
        // JWT timestamps have second precision; truncate so expiresAt matches the token exactly
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(properties.jwt().accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.id())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("email", user.email())
                .claim("name", user.name())
                .claim(ROLE_CLAIM, user.role().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }
}
