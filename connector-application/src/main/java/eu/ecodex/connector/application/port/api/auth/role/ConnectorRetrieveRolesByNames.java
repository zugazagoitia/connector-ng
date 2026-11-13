/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.port.api.auth.role;

import eu.ecodex.connector.application.exception.role.ConnectorRoleNotFoundException;
import eu.ecodex.connector.domain.model.user.ConnectorRole;
import jakarta.annotation.Nonnull;
import java.util.Set;

/**
 * Defines a contract for retrieving user roles within the Connector system.
 * Implementations of this interface are responsible for fetching user role details
 * based on either a unique identifier or a username.
 *
 * <p>This interface is primarily used to locate and retrieve specific {@link ConnectorRole}
 * entities from the underlying persistence mechanism, allowing system components to work
 * with role information associated with connector users.
 */
public interface ConnectorRetrieveRolesByNames {
    /**
     * Retrieves a user role identified by the specified name.
     * This method fetches the {@link ConnectorRole} entity from the
     * underlying data source using the provided role name.
     *
     * @param roleNames the name of the user roles to be retrieved.
     *                  It must correspond to an existing user role in the system.
     *
     * @return the Set of {@link ConnectorRole} instance matching the given roles name.
     *
     * @throws ConnectorRoleNotFoundException if no user role is found for the specified name.
     */
    Set<ConnectorRole> execute(@Nonnull Set<String> roleNames);
}
