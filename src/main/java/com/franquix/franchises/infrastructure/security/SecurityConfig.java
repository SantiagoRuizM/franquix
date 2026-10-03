package com.franquix.franchises.infrastructure.security;

import com.franquix.franchises.infrastructure.entrypoint.rest.error.ProblemDetailWriter;
import com.franquix.franchises.infrastructure.security.ratelimit.FixedWindowRateLimiter;
import com.franquix.franchises.infrastructure.security.ratelimit.RateLimitWebFilter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.core.convert.converter.Converter;
import reactor.core.publisher.Mono;

/**
 * Stateless bearer-token security.
 *
 * <ul>
 *   <li>Public: login, refresh, health, API docs.</li>
 *   <li>{@code GET /api/v1/franchises/**}: ADMIN or VIEWER.</li>
 *   <li>Any other franchise operation and {@code /api/v1/users/**}: ADMIN.</li>
 *   <li>Everything else (logout, profile, password change): any authenticated user.</li>
 * </ul>
 *
 * <p>No sessions, cookies or CSRF tokens: every request carries its own JWT.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    static final String[] PUBLIC_PATHS = {
        "/actuator/health", "/actuator/health/**", "/actuator/info",
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/webjars/**"
    };

    @Bean
    SecretKey jwtSigningKey(SecurityProperties properties) {
        return new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    /** Verifies signature, expiry (60 s clock skew) and issuer. */
    @Bean
    ReactiveJwtDecoder jwtDecoder(SecretKey jwtSigningKey, SecurityProperties properties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
        return decoder;
    }

    @Bean
    FixedWindowRateLimiter rateLimiter(Clock clock) {
        return new FixedWindowRateLimiter(clock);
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            ProblemDetailWriter writer,
            FixedWindowRateLimiter rateLimiter,
            SecurityProperties properties) {
        ServerAuthenticationEntryPoint unauthorized = unauthorized(writer);
        ServerAccessDeniedHandler forbidden = (exchange, denied) ->
                writer.write(exchange, HttpStatus.FORBIDDEN, "You do not have permission to perform this operation");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/franchises", "/api/v1/franchises/**")
                        .hasAnyRole("ADMIN", "VIEWER")
                        .pathMatchers("/api/v1/franchises", "/api/v1/franchises/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder).jwtAuthenticationConverter(roleClaimConverter()))
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .addFilterAfter(new RateLimitWebFilter(rateLimiter, properties.rateLimit(), writer),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /** Maps the {@code role} claim to a {@code ROLE_*} authority. */
    private static Converter<Jwt, Mono<AbstractAuthenticationToken>> roleClaimConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(JwtAccessTokenIssuer.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    private static ServerAuthenticationEntryPoint unauthorized(ProblemDetailWriter writer) {
        return (exchange, failure) -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            return writer.write(exchange,
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                            "Authentication required: missing, invalid or expired access token"),
                    headers);
        };
    }
}
