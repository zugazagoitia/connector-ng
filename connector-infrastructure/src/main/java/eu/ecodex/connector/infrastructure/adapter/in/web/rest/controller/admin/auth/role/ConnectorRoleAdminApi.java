/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.auth.role;

import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorRoleDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * API interface for managing connector user roles.
 *
 * <p>This interface exposes endpoints for creating, updating, retrieving, and deleting
 * roles assigned to connector users. It allows for management of the roles within the connector's
 * authorization and role-based access control system.
 *
 * <p>All endpoints consume and produce JSON data.
 */
@RequestMapping(path = "/api/v1/admin/users/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "AdministrateRoles", description = "API for managing connector's users roles")
public interface ConnectorRoleAdminApi {
    @Operation(summary = "Retrieve all connector's user roles.")
    @GetMapping
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    List<ConnectorRoleDto> listRoles();
}
