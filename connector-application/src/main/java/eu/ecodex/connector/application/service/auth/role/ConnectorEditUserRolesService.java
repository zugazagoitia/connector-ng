/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.auth.role;

import eu.ecodex.connector.application.exception.role.ConnectorRoleNotFoundException;
import eu.ecodex.connector.application.exception.user.ConnectorUserNotFoundException;
import eu.ecodex.connector.application.port.api.auth.role.ConnectorRetrieveRoleByName;
import eu.ecodex.connector.application.port.api.auth.role.ConnectorRetrieveRolesByNames;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUserRoles;
import eu.ecodex.connector.application.port.spi.auth.role.ConnectorRoleRepository;
import eu.ecodex.connector.application.port.spi.auth.user.ConnectorUserRepository;
import eu.ecodex.connector.domain.model.user.ConnectorRole;
import eu.ecodex.connector.domain.model.user.ConnectorRoleName;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import java.util.Set;
import java.util.TreeSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Service implementation for registering and assigning roles to users within the Connector system.
 * This class is responsible for creating new user roles or updating existing ones based on the
 * provided identifier and role name. It serves as the bridge between the application's business
 * logic
 * and the persistence layer through the {@link ConnectorRoleRepository}.
 *
 * <p>Responsibilities:
 * - Handles the creation and registration of new user roles in the data source.
 * - Updates existing user role assignments when applicable.
 * - Ensures that the provided role and user identifiers are valid, throwing exceptions
 * if the required entities are not found.
 *
 * <p>Exceptions:
 * - {@link ConnectorRoleNotFoundException}: Thrown if the specified role does not exist.
 * - {@link ConnectorUserNotFoundException}: Thrown if the specified user identifier does not
 * exist.
 */
@Slf4j
@Component
@Transactional
public class ConnectorEditUserRolesService implements ConnectorEditUserRoles {
    private static final String DEFAULT_ROLE_NAME = ConnectorRoleName.USER.prefixedWithRole();
    private final ConnectorRetrieveRolesByNames retrieveRolesByNames;
    private final ConnectorRetrieveRoleByName retrieveRoleByName;
    private final ConnectorUserRepository userRepository;

    /**
     * Construct edit user roles service.
     *
     * @param retrieveRolesByNames the service used to retrieve list of roles from database
     * @param retrieveRoleByName   the service used to retrieve a specific role. throws NotFound
     *                             exception if not found.
     * @param userRepository       the repository use to update user information
     */
    public ConnectorEditUserRolesService(
        ConnectorRetrieveRolesByNames retrieveRolesByNames,
        ConnectorRetrieveRoleByName retrieveRoleByName,
        ConnectorUserRepository userRepository) {
        this.retrieveRolesByNames = retrieveRolesByNames;
        this.retrieveRoleByName = retrieveRoleByName;
        this.userRepository = userRepository;
    }

    private ConnectorUser getUserByIdentifier(String identifier) {
        return userRepository.findByUuid(identifier)
            .orElseThrow(() -> new ConnectorUserNotFoundException(identifier));
    }

    @Override
    public ConnectorUser execute(@NonNull String userIdentifier,
                                 @NonNull Set<String> roles) {

        var user = getUserByIdentifier(userIdentifier);

        if (roles.isEmpty()) {
            return assignDefaultRoleIfEmpty(user);
        }

        // there is nothing to update
        if (!CollectionUtils.isEmpty(user.roles())) {
            var currentRoles = user.roleNames();
            if (currentRoles.equals(roles)) {
                return user;
            }
        }

        var foundRoles = retrieveRolesByNames.execute(roles);

        var missingRoles = new TreeSet<>(roles);
        foundRoles.stream().map(ConnectorRole::name).forEach(missingRoles::remove);

        if (!missingRoles.isEmpty()) {
            log.debug("Unknown user role name(s) :[{}]", String.join("", missingRoles));

            if (foundRoles.isEmpty()) {
                assignDefaultRoleIfEmpty(user);
                throw new ConnectorRoleNotFoundException(
                    "Unknown role name(s): %s".formatted(String.join(", ", missingRoles)));
            }
        }

        var updated = user.editRoles(foundRoles);
        return (user.equals(updated)) ? user : userRepository.save(updated);
    }

    private ConnectorUser assignDefaultRoleIfEmpty(ConnectorUser user) {
        if (CollectionUtils.isEmpty(user.roles())) {
            log.debug("Default role is not configured, adding default user role to user [{}]",
                user.uuid());
            var defaultUserRole = retrieveRoleByName.execute(DEFAULT_ROLE_NAME);
            var updated = user.addRole(defaultUserRole);
            return userRepository.save(updated);
        }
        return user;
    }
}
