package com.franquix.franchises.domain.port.out;

import com.franquix.franchises.domain.model.auth.AccessToken;
import com.franquix.franchises.domain.model.auth.User;
import reactor.core.publisher.Mono;

/** Issues signed, short-lived access tokens carrying the user's identity and role. */
public interface AccessTokenIssuerPort {

    Mono<AccessToken> issue(User user);
}
