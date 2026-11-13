/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.auth;

import eu.ecodex.connector.domain.model.auth.ConnectorUserAuthenticationResult;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.login.ConnectorLoginRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.login.ConnectorRefreshTokenRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.login.ConnectorUpdateUserPasswordRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.logout.ConnectorLogoutRequest;
import eu.ecodex.connector.infrastructure.adapter.out.auth.identity.ConnectorUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Defines the API for managing user login functionality within the connector system. Provides an
 * operation for authenticating users and retrieving an access token upon successful login. This
 * interface serves as a contract for the implementation of user login services.
 *
 * <p>Endpoints:
 * - POST /api/v1/auth/login: Handles user login by accepting credentials in the request body and
 * responding with an authentication token.
 */
@RequestMapping("/api/v1/auth")
@Tag(name = "AuthenticateUser", description = "API for managing connector's users "
    + "authentication")
public interface ConnectorAuthenticationApi {
    @Operation(summary = "Login a connector user.")
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ConnectorUserAuthenticationResult login(
        @RequestBody @Valid @Nonnull ConnectorLoginRequest connectorLoginRequest);

    @Operation(summary = "Refresh a user token.")
    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    ConnectorUserAuthenticationResult refreshJwtToken(
        @RequestBody @Valid @Nonnull ConnectorRefreshTokenRequest request);

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update a connector user password after login, using access and refresh "
        + "tokens.")
    @PostMapping(path = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not Found")
    void updatePasswordAfterLogin(
        @AuthenticationPrincipal @Nonnull ConnectorUserDetails userDetails,
        @Valid @RequestBody @Nonnull ConnectorUpdateUserPasswordRequest userRequest);

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout a user token.")
    @PostMapping("/logout")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    void logout(
        @AuthenticationPrincipal @Nonnull ConnectorUserDetails userDetails,
        @RequestBody @Valid @Nonnull ConnectorLogoutRequest request);
}
