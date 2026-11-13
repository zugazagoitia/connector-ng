/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.user;

import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.login.ConnectorUpdateUserPasswordRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditSelfRequest;
import eu.ecodex.connector.infrastructure.adapter.out.auth.identity.ConnectorUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Interface for managing connector users via REST APIs. Provides operations for patching and
 * retrieving a user. This interface defines the contract for user management-related endpoints.
 */
@RequestMapping(path = "/api/v1/auth/me", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "AuthenticateUserProfile", description = "API for managing connector's current user")
public interface ConnectorUserApi {
    @Operation(summary = "Update partially the currently connected user profile.")
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @ApiResponse(responseCode = "409", description = "Conflict")
    ConnectorUserDto editAuthenticatedUser(
        @AuthenticationPrincipal @Nonnull ConnectorUserDetails userDetails,
        @Valid @RequestBody @Nonnull ConnectorEditSelfRequest userRequest);

    @Operation(summary = "Get the currently authenticated user account.")
    @GetMapping
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ConnectorUserDto retrieveAuthenticatedUser(
        @AuthenticationPrincipal @Nonnull ConnectorUserDetails userDetails);

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update a connector user password after login, using access and refresh "
        + "tokens.")
    @PostMapping(path = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not Found")
    void updateAuthenticatedUserPassword(
        @AuthenticationPrincipal @Nonnull ConnectorUserDetails userDetails,
        @Valid @RequestBody @Nonnull ConnectorUpdateUserPasswordRequest userPasswordRequest);
}
