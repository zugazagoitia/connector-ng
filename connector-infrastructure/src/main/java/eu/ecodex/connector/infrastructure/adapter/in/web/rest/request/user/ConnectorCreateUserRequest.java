/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user;

import eu.ecodex.connector.domain.model.user.ConnectorRole;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;


/**
 * Represents a request to create a new user in the connector system.
 *
 * @param username the username (must not be blank, at most 255 characters)
 * @param password the clear-text password, to be hashed by the use case; between 6 and 255
 *                 characters if provided; may be {@code null}
 * @param email    the email address; must be a valid email address of at most 255 characters if
 *                 provided; may be {@code null}
 * @param enabled  whether the user account is enabled (must not be null)
 * @param roles    the names of the roles to assign to the user; each name must not be blank; may be
 *                 {@code null} or empty
 */
@Builder(toBuilder = true)
public record ConnectorCreateUserRequest(
    @NotBlank(message = "The [username] must not be blank")
    @Size(max = 255, message = "The [username] must not exceed 255 characters")
    String username,

    @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
    String password,

    @Email(message = "The [email] must be a valid email address")
    @Size(max = 255, message = "The [email] must not exceed 255 characters")
    String email,

    @NotNull(message = "The [enabled] must not be null")
    Boolean enabled,

    Set<@NotBlank(message = "Role must not be blank") String> roles
) {
    /**
     * Maps a user creation or update request to a domain user.
     *
     * @param userRequest the {@link ConnectorCreateUserRequest} to map (must not be
     *                    null)
     *
     * @return the {@link ConnectorUser} built from the request
     */
    public static ConnectorUser toDomain(ConnectorCreateUserRequest userRequest) {
        return ConnectorUser.builder()
            .username(userRequest.username())
            .password(userRequest.password())
            .email(userRequest.email())
            .enabled(userRequest.enabled())
            .roles(getRoles(userRequest))
            .build();
    }

    private static Set<ConnectorRole> getRoles(ConnectorCreateUserRequest request) {
        if (request.roles() == null) {
            return Set.of();
        }

        return request.roles().stream()
            .map(role ->
                ConnectorRole.builder()
                    .name(role)
                    .build())
            .collect(Collectors.toUnmodifiableSet());
    }
}
