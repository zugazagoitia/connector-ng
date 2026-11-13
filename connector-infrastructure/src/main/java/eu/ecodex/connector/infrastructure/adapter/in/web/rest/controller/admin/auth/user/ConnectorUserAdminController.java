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

import static eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorCreateUserRequest.toDomain;

import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorListUsers;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRegisterUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRemoveUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRetrieveUserByIdentifier;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorCreateUserRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditUserRequest;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing connector users. Provides APIs for operations such as registration,
 * updating, partial updates, retrieval, listing, and deletion of users.
 *
 * <p>This controller relies on service classes for handling user-related operations and
 * ensures additional processing like password encoding before delegation.
 */
@Slf4j
@RestController
public class ConnectorUserAdminController implements ConnectorUserAdminApi {
    private final ConnectorRetrieveUserByIdentifier connectorRetrieveUserByIdentifier;
    private final ConnectorRegisterUser connectorRegisterUser;
    private final ConnectorEditUser connectorEditUser;
    private final ConnectorRemoveUser connectorRemoveUser;
    private final ConnectorListUsers connectorListUsers;

    /**
     * Constructs an instance of {@code ConnectorUserAdminController} with the required service
     * interfaces for managing {@link ConnectorUser} entities.
     *
     * @param connectorRetrieveUserByIdentifier service for retrieving {@link ConnectorUser}
     *                                          entities by identifier
     * @param connectorRegisterUser             service for registering new {@link ConnectorUser}
     *                                          entities in the system
     * @param connectorEditUser                 service for partially updating {@link ConnectorUser}
     *                                          entities by applying specified changes
     * @param connectorRemoveUser               service for deleting {@link ConnectorUser} entities
     *                                          from the system
     * @param connectorListUsers                service for retrieving a list of all
     *                                          {@link ConnectorUser} entities along with their
     *                                          roles
     */
    public ConnectorUserAdminController(
        ConnectorRetrieveUserByIdentifier connectorRetrieveUserByIdentifier,
        ConnectorRegisterUser connectorRegisterUser,
        ConnectorEditUser connectorEditUser,
        ConnectorRemoveUser connectorRemoveUser,
        ConnectorListUsers connectorListUsers) {
        this.connectorRetrieveUserByIdentifier = connectorRetrieveUserByIdentifier;
        this.connectorRegisterUser = connectorRegisterUser;
        this.connectorEditUser = connectorEditUser;
        this.connectorRemoveUser = connectorRemoveUser;
        this.connectorListUsers = connectorListUsers;
    }

    @Override
    public ConnectorUserDto registerUser(
        @NonNull ConnectorCreateUserRequest userRequest) {
        log.debug("Registering new user");
        var registered = connectorRegisterUser.execute(toDomain(userRequest));

        log.debug("New user registered successfully");
        return ConnectorUserDto.from(registered);
    }

    @Override
    public ConnectorUserDto editUser(
        @NonNull String identifier,
        @NonNull ConnectorEditUserRequest userRequest) {
        log.debug("Editing existing user");
        var registered = connectorEditUser.execute(
            identifier,
            ConnectorEditUserRequest.toCommand(identifier, userRequest)
        );

        log.debug("User edited successfully");
        return ConnectorUserDto.from(registered);
    }

    @Override
    public ConnectorUserDto retrieveUser(@NonNull String identifier) {
        ConnectorUser userById = connectorRetrieveUserByIdentifier.execute(identifier);
        return ConnectorUserDto.from(userById);
    }

    @Override
    public List<ConnectorUserDto> listUsers() {
        return connectorListUsers.execute().stream().map(ConnectorUserDto::from).toList();
    }

    @Override
    public Void removeUser(@NonNull String userIdentifier) {
        connectorRemoveUser.execute(userIdentifier);
        log.debug("User deleted by identifier");

        return null;
    }
}
