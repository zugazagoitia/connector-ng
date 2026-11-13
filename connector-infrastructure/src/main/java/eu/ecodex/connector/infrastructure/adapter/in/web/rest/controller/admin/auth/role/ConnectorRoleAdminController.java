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

import eu.ecodex.connector.application.port.api.auth.role.ConnectorListRole;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorRoleDto;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for managing user roles within the connector system. Provides
 * functionality to create, update, retrieve, list, and delete user roles. Implements the API
 * defined in {@link ConnectorRoleAdminApi}. Uses service-level components to execute business logic
 * related to user roles.
 */
@Slf4j
@RestController
public class ConnectorRoleAdminController implements ConnectorRoleAdminApi {
    private final ConnectorListRole connectorListRole;

    /**
     * Creates a new instance of {@link ConnectorRoleAdminController}.
     *
     * @param connectorListRole The role listing service.
     */
    public ConnectorRoleAdminController(
        ConnectorListRole connectorListRole) {
        this.connectorListRole = connectorListRole;
    }


    @Override
    public List<ConnectorRoleDto> listRoles() {
        return connectorListRole.execute().stream()
            .map(ConnectorRoleDto::from)
            .toList();
    }
}
