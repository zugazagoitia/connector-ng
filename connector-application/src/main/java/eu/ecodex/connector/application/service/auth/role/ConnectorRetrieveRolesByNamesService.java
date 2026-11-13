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

import eu.ecodex.connector.application.port.api.auth.role.ConnectorRetrieveRolesByNames;
import eu.ecodex.connector.application.port.spi.auth.role.ConnectorRoleRepository;
import eu.ecodex.connector.domain.model.user.ConnectorRole;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service implementation for retrieving user roles within the Connector system. This class provides
 * methods to fetch user role details based on roles name. It interacts with
 * the {@link ConnectorRoleRepository} to query the underlying data source.
 */
@Slf4j
@Service
public class ConnectorRetrieveRolesByNamesService implements ConnectorRetrieveRolesByNames {
    private final ConnectorRoleRepository repository;

    public ConnectorRetrieveRolesByNamesService(ConnectorRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<ConnectorRole> execute(@NonNull Set<String> roleNames) {
        return repository.findByNameIn(roleNames);
    }
}
