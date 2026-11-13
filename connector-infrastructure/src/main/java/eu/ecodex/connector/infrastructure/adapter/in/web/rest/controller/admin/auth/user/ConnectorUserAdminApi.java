/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.auth.user;

import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorCreateUserRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Interface for managing connector users via REST APIs. Provides operations for creating, updating,
 * patching, retrieving, listing, and deleting users. This interface defines the contract for user
 * management-related endpoints.
 */
@RequestMapping(path = "/api/v1/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "AdministrateUsers", description = "API for managing connector's users")
public interface ConnectorUserAdminApi {
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Persist a connector user.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @ApiResponse(responseCode = "409", description = "Conflict")
    ConnectorUserDto registerUser(
        @Valid @RequestBody @Nonnull ConnectorCreateUserRequest userRequest);

    @Operation(summary = "Update partially a connector user.")
    @PatchMapping(path = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @ApiResponse(responseCode = "409", description = "Conflict")
    ConnectorUserDto editUser(
        @PathVariable("uuid") @Nonnull String identifier,
        @Valid @RequestBody @Nonnull ConnectorEditUserRequest userRequest);

    @Operation(summary = "Retrieve a connector user.")
    @GetMapping(path = "/{uuid}")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ConnectorUserDto retrieveUser(@PathVariable("uuid") @Nonnull String identifier);

    @Operation(summary = "Retrieve all connector's users.")
    @GetMapping
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    List<ConnectorUserDto> listUsers();

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a connector user by Identifier.")
    @DeleteMapping(path = "/{uuid}")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not Found")
    Void removeUser(@PathVariable("uuid") @Nonnull String userIdentifier);
}
