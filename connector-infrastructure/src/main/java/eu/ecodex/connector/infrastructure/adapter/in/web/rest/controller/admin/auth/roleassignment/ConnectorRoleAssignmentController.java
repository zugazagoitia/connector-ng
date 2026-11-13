/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.auth.roleassignment;

import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUserRoles;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditUserRolesRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing connector role assignments.
 *
 * <p>This controller provides endpoints to update user-role associations within the connector
 * system. It acts as an implementation of the {@link ConnectorRoleAssignmentAdminApi}
 * interface and utilizes the {@link ConnectorEditUserRoles} service for performing the core
 * business logic related to role management.
 *
 * <p>Responsibilities:
 * Update of a user roles in the connector system. - Deleting of roles previously assigned roles
 * to a user.
 *
 */
@Slf4j
@RestController
public class ConnectorRoleAssignmentController implements ConnectorRoleAssignmentAdminApi {
    private final ConnectorEditUserRoles editUserRoles;

    public ConnectorRoleAssignmentController(ConnectorEditUserRoles editUserRoles) {
        this.editUserRoles = editUserRoles;
    }

    @Override
    public ConnectorUserDto editUserRoles(@NonNull String userIdentifier,
                                          ConnectorEditUserRolesRequest userRolesRequest) {
        var connectorUser = editUserRoles.execute(userIdentifier, userRolesRequest.roles());
        return ConnectorUserDto.from(connectorUser);
    }
}
