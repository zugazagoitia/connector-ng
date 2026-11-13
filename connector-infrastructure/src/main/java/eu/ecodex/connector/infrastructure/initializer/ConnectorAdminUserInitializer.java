/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.initializer;

import static eu.ecodex.connector.domain.model.user.ConnectorRole.DEFAULT_ADMIN_ROLE;
import static eu.ecodex.connector.domain.model.user.ConnectorRole.defaultAdminRole;
import static eu.ecodex.connector.domain.model.user.ConnectorRole.defaultLoadTesterRole;
import static eu.ecodex.connector.domain.model.user.ConnectorRole.defaultUserRole;

import eu.ecodex.connector.application.exception.role.ConnectorRoleAlreadyExistsException;
import eu.ecodex.connector.application.exception.user.ConnectorUserAlreadyExistsException;
import eu.ecodex.connector.application.exception.user.ConnectorUserNotFoundException;
import eu.ecodex.connector.application.port.api.auth.role.ConnectorRegisterRole;
import eu.ecodex.connector.application.port.api.auth.role.ConnectorRetrieveRoleByName;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUserRoles;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRegisterUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRetrieveUserByUsername;
import eu.ecodex.connector.domain.model.user.ConnectorRole;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import eu.ecodex.connector.infrastructure.config.property.auth.ConnectorAdminUserProperties;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Initializes an admin user in the connector system during the application startup phase.
 * The class is designed to ensure that a default or configured admin user exists with appropriate
 * roles.
 *
 * <p>This initializer performs the following steps:
 * 1. Checks if admin user properties are provided in the configuration.
 * 2. If no properties are provided, attempts to register a fallback default admin user.
 * 3. If properties are provided, uses them to initialize the admin user with the configured
 * username, password, email, and role.
 *
 * <p>Key operations:
 * - Registers a new admin role if it does not already exist.
 * - Handles situations where the admin user or default admin role already exists.
 * - Updates an existing user with administrative privileges if necessary.
 *
 * <p>Dependencies:
 * - {@link ConnectorRegisterUser}: Service for registering and updating user information.
 * - {@link ConnectorRegisterRole}: Service for registering user roles.
 * - {@link ConnectorRetrieveUserByUsername}: Service for retrieving existing user details.
 * - {@link ConnectorAdminUserProperties}: Configuration properties for the admin user.
 *
 * <p>Implements:
 * - {@link ApplicationRunner}: Allows the initialization logic to execute upon application
 * startup.
 *
 * <p>Logging:
 * - Logs events and outcomes during the initialization process for traceability and debugging.
 */
@Slf4j
@Component
public class ConnectorAdminUserInitializer implements ApplicationRunner {
    private final ConnectorRegisterUser registerUser;
    private final ConnectorRegisterRole registerRole;
    private final ConnectorEditUserRoles editUserRoles;
    private final ConnectorRetrieveUserByUsername retrieveUserByUsername;
    private final ConnectorRetrieveRoleByName retrieveUserRoleByName;
    private final ConnectorAdminUserProperties adminUserProperties;

    /**
     * Initializes and configures the admin user for the Connector system.
     * This constructor sets up the required dependencies for managing user and role initialization.
     *
     * @param registerUser           the {@link ConnectorRegisterUser} instance responsible for
     *                               registering new users.
     * @param registerRole           the {@link ConnectorRegisterRole} instance responsible for
     *                               registering user roles.
     * @param retrieveUserByUsername the {@link ConnectorRetrieveUserByUsername} instance used to
     *                               retrieve users by their username.
     * @param retrieveUserRoleByName the {@link ConnectorRetrieveRoleByName} instance used to
     *                               retrieve user roles by their name.
     * @param adminUserProperties    the {@link ConnectorAdminUserProperties} object containing
     *                               configuration properties for the admin user.
     */
    public ConnectorAdminUserInitializer(ConnectorRegisterUser registerUser,
                                         ConnectorRegisterRole registerRole,
                                         ConnectorEditUserRoles editUserRoles,
                                         ConnectorRetrieveUserByUsername retrieveUserByUsername,
                                         ConnectorRetrieveRoleByName retrieveUserRoleByName,
                                         ConnectorAdminUserProperties adminUserProperties) {
        this.registerUser = registerUser;
        this.registerRole = registerRole;
        this.editUserRoles = editUserRoles;
        this.retrieveUserByUsername = retrieveUserByUsername;
        this.retrieveUserRoleByName = retrieveUserRoleByName;
        this.adminUserProperties = adminUserProperties;
    }


    @Override
    public void run(@NonNull ApplicationArguments args) {
        initializeDefaultUserRoles();

        if (adminUserProperties == null) {
            log.info("No Administrator user configured in properties");
            registerFallbackAdminUser();
            return;
        }
        initializeAdminUser(adminUserProperties);
    }

    private void initializeDefaultUserRoles() {
        registerDefaultRole(defaultUserRole());
        registerDefaultRole(defaultAdminRole());
        registerDefaultRole(defaultLoadTesterRole());
    }

    private void registerFallbackAdminUser() {
        log.info("No Administrator user found in configuration; creating default admin user");
        ConnectorUser existingAdmin;
        try {
            existingAdmin = retrieveUserByUsername.execute(ConnectorUser.DEFAULT_ADMIN_USER_NAME);
            if (existingAdmin.isDefaultAdmin()) {
                log.info("Default Administrator user already exists.");
                return;
            }
            updateWithAdminRole(existingAdmin);
        } catch (ConnectorUserNotFoundException e) {
            registerNewAdminUser();
        }
    }

    private void updateWithAdminRole(ConnectorUser administrator) {
        log.info("Administrator user exists but has not admin role; adding {}", DEFAULT_ADMIN_ROLE);

        editUserRoles.execute(administrator.uuid(), Set.of(DEFAULT_ADMIN_ROLE));
        log.info("{} added to Administrator user, admin user updated", DEFAULT_ADMIN_ROLE);
    }

    private void registerNewAdminUser() {
        log.info(
            "No default Administrator user found and none registered yet; creating "
                + "default");

        var defaultAdminUser = ConnectorUser.defaultAdminUser();
        registerUser.execute(defaultAdminUser);
    }

    private void registerDefaultRole(ConnectorRole defaultRole) {
        try {
            registerRole.execute(defaultRole);
            log.info("Default {} successfully created.", defaultRole.name());
        } catch (ConnectorRoleAlreadyExistsException e) {
            log.info("Default {} already exists.", defaultRole.name());
        }
    }

    private void initializeAdminUser(ConnectorAdminUserProperties properties) {
        if (!StringUtils.hasText(properties.getUsername())) {
            registerFallbackAdminUser();
            return;
        }

        try {
            log.info("Registering Administrator user - username [{}]", properties.getUsername());
            var adminRole = retrieveUserRoleByName.execute(DEFAULT_ADMIN_ROLE);
            registerUser.execute(createAdminUser(properties, adminRole));
            log.info("Administrator user [{}] successfully registered.", properties.getUsername());
        } catch (ConnectorUserAlreadyExistsException e) {
            log.info("Administrator user [{}] already registered.", properties.getUsername());
        }
    }

    private ConnectorUser createAdminUser(ConnectorAdminUserProperties properties,
                                          ConnectorRole adminRole) {
        return ConnectorUser.builder()
            .username(properties.getUsername())
            .password(properties.getPassword())
            .email(properties.getEmail())
            .enabled(Boolean.TRUE)
            // The default admin user will be required to change its password at first login.
            .mustChangePassword(Boolean.TRUE)
            .roles(Set.of(adminRole))
            .build();
    }
}
