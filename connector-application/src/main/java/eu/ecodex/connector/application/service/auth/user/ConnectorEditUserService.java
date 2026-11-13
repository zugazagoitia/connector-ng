/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.auth.user;

import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUserCommand;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRetrieveUserByIdentifier;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorVerifyUniqueUserEmail;
import eu.ecodex.connector.application.port.spi.auth.user.ConnectorUserRepository;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Service implementation for managing the registration and updates of {@link ConnectorUser}
 * entities.
 * This class provides methods for creating, updating, and partially updating user information while
 * ensuring
 * data integrity and validation.
 *
 * <p>It handles the following operations:
 * - Validating user data before registration or updates.
 * - Enforcing uniqueness constraints on usernames and email addresses.
 * - Performing full or partial updates on existing users.
 *
 * <p>The class is annotated with {@code @Component} to indicate that it's a Spring-managed bean and
 * {@code @Slf4j} for logging purposes. It requires {@link ConnectorUserRepository} as a dependency
 * to perform data access operations.
 */
@Slf4j
@Component
public class ConnectorEditUserService implements ConnectorEditUser {
    private final ConnectorUserRepository repository;
    private final ConnectorVerifyUniqueUserEmail verifierUniqueUserEmail;
    private final ConnectorRetrieveUserByIdentifier retrieveUserByIdentifier;

    /**
     * Constructs an instance of {@code ConnectorPatchUserService} with the required dependencies.
     *
     * @param repository               the repository used for user data operations; must not be
     *                                 null
     * @param verifierUniqueUserEmail  the service to verify user email uniqueness; must not be null
     * @param retrieveUserByIdentifier the service to retrieve a user by its unique identifier; must
     *                                 not be null
     */
    public ConnectorEditUserService(
        ConnectorUserRepository repository,
        ConnectorVerifyUniqueUserEmail verifierUniqueUserEmail,
        ConnectorRetrieveUserByIdentifierService retrieveUserByIdentifier) {
        this.repository = repository;
        this.verifierUniqueUserEmail = verifierUniqueUserEmail;
        this.retrieveUserByIdentifier = retrieveUserByIdentifier;
    }

    /**
     * Patches the specified {@link ConnectorUser} with the provided fields.
     * The roles field cannot be patched.
     *
     * @param identifier the unique identifier of the {@link ConnectorUser} to be patched; must not
     *                   be null
     * @param user       the {@link ConnectorUser} object containing the fields to update; must not
     *                   be null
     *
     * @return patched {@link ConnectorUser} object
     */
    @Override
    public ConnectorUser execute(@NonNull String identifier,
                                 @NonNull ConnectorEditUserCommand user) {
        var existingUser = retrieveUserByIdentifier.execute(identifier);

        verifierUniqueUserEmail.execute(ConnectorEditUserCommand.toDomain(user));

        var userBuilder = existingUser.toBuilder();

        if (StringUtils.hasText(user.email())) {
            userBuilder.email(user.email());
        }

        if (user.enabled() != null) {
            userBuilder.enabled(user.enabled());
        }

        if (user.mustChangePassword() != null) {
            userBuilder.mustChangePassword(user.mustChangePassword());
        }
        return repository.save(userBuilder.build());
    }
}
