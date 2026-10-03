package com.franquix.franchises.infrastructure.entrypoint.rest.router;

import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.ChangePasswordRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.CreateUserRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.LoginRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.RefreshTokenRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.TokenResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.UserResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.AuthHandler;
import com.franquix.franchises.infrastructure.entrypoint.rest.handler.UserHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/** Authentication and user management routes. Access rules live in {@code SecurityConfig}. */
@Configuration(proxyBeanMethods = false)
public class AuthRouter {

    public static final String AUTH_PATH = "/api/v1/auth";
    public static final String USERS_PATH = "/api/v1/users";

    @Bean
    @RouterOperations({
            @RouterOperation(path = AUTH_PATH + "/login", method = RequestMethod.POST,
                    beanClass = AuthHandler.class, beanMethod = "login",
                    operation = @Operation(operationId = "login", tags = "Authentication",
                            summary = "Log in with email and password",
                            description = "Returns a 5-minute access token and a 1-day single-use refresh token. "
                                    + "Limited to 5 attempts per minute per client IP.",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = LoginRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Token pair",
                                            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "401", ref = "Unauthorized"),
                                    @ApiResponse(responseCode = "429", ref = "TooManyRequests")})),
            @RouterOperation(path = AUTH_PATH + "/refresh", method = RequestMethod.POST,
                    beanClass = AuthHandler.class, beanMethod = "refresh",
                    operation = @Operation(operationId = "refreshSession", tags = "Authentication",
                            summary = "Exchange a refresh token for a new token pair",
                            description = "The refresh token is rotated: it works once. Presenting an already "
                                    + "used token revokes every session of its user.",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "New token pair",
                                            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "401", ref = "Unauthorized"),
                                    @ApiResponse(responseCode = "429", ref = "TooManyRequests")})),
            @RouterOperation(path = AUTH_PATH + "/logout", method = RequestMethod.POST,
                    beanClass = AuthHandler.class, beanMethod = "logout",
                    operation = @Operation(operationId = "logout", tags = "Authentication",
                            summary = "Revoke a refresh token of the current user",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "204", description = "Revoked (idempotent)"),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "401", ref = "Unauthorized")})),
            @RouterOperation(path = AUTH_PATH + "/me", method = RequestMethod.GET,
                    beanClass = AuthHandler.class, beanMethod = "me",
                    operation = @Operation(operationId = "currentUser", tags = "Authentication",
                            summary = "Profile of the authenticated user",
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Current user",
                                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                                    @ApiResponse(responseCode = "401", ref = "Unauthorized")})),
            @RouterOperation(path = AUTH_PATH + "/password", method = RequestMethod.PATCH,
                    beanClass = AuthHandler.class, beanMethod = "changePassword",
                    operation = @Operation(operationId = "changePassword", tags = "Authentication",
                            summary = "Change the current user's password",
                            description = "Requires the current password and ends every existing session.",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = ChangePasswordRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "204", description = "Password changed"),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "401", ref = "Unauthorized"),
                                    @ApiResponse(responseCode = "429", ref = "TooManyRequests")})),
            @RouterOperation(path = USERS_PATH, method = RequestMethod.POST,
                    beanClass = UserHandler.class, beanMethod = "create",
                    operation = @Operation(operationId = "createUser", tags = "Users",
                            summary = "Create a user (ADMIN only)",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = CreateUserRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "User created",
                                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                                    @ApiResponse(responseCode = "400", ref = "BadRequest"),
                                    @ApiResponse(responseCode = "401", ref = "Unauthorized"),
                                    @ApiResponse(responseCode = "403", ref = "Forbidden"),
                                    @ApiResponse(responseCode = "409", ref = "Conflict")}))
    })
    RouterFunction<ServerResponse> authRoutes(AuthHandler auth, UserHandler users) {
        return RouterFunctions.route()
                .POST(AUTH_PATH + "/login", auth::login)
                .POST(AUTH_PATH + "/refresh", auth::refresh)
                .POST(AUTH_PATH + "/logout", auth::logout)
                .GET(AUTH_PATH + "/me", auth::me)
                .PATCH(AUTH_PATH + "/password", auth::changePassword)
                .POST(USERS_PATH, users::create)
                .build();
    }
}
