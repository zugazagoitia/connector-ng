/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.domain.model.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;

/**
 * Represents a data structure for a user in the Connector system.
 *
 * <p>This class provides information about the user, such as an identifier, username,
 * password, email, and roles, and includes metadata such as enabled status, creation
 * time, and last updated time.
 * It also encapsulates behavior for content comparison
 * and builder for creating immutable instances of the class.
 *
 */
@Builder(toBuilder = true)
public record ConnectorUser(
    String uuid,
    @NotBlank
    String username,
    String password,
    String email,
    boolean enabled,
    boolean mustChangePassword,
    Set<ConnectorRole> roles,
    Instant createdAt,
    Instant updatedAt
) {

    public static final String DEFAULT_ADMIN_USER_NAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "123456";

    /**
     * Create default administrator user.
     *
     * @return Default administrator
     */
    public static ConnectorUser defaultAdminUser() {
        return ConnectorUser.builder()
            .username(DEFAULT_ADMIN_USER_NAME)
            .password(DEFAULT_ADMIN_PASSWORD)
            .enabled(Boolean.TRUE)
            .mustChangePassword(Boolean.TRUE)
            .roles(Set.of(ConnectorRole.defaultAdminRole()))
            .build();
    }

    /**
     * Check if the user is the default admin.
     *
     * @return true if the user is the default admin.
     */
    public boolean isDefaultAdmin() {
        return DEFAULT_ADMIN_USER_NAME.equals(username) && roles != null && roles
            .stream()
            .anyMatch(ConnectorRole::isDefaultAdminRole);
    }

    /**
     * Replace the current user roles with new roles in parameter.
     *
     * @param newRoles new roles to apply
     *
     * @return updated user
     */
    public ConnectorUser editRoles(@NotEmpty Set<ConnectorRole> newRoles) {
        if (newRoles.isEmpty()) {
            return this;
        }
        var updatedRoles = new HashSet<>(newRoles);
        return toBuilder().roles(updatedRoles).build();
    }

    /**
     * Add a new role to the current user.
     *
     * @param role new role to add
     *
     * @return updated user
     */
    public ConnectorUser addRole(ConnectorRole role) {
        var updatedRoles = roles == null
            ? new HashSet<ConnectorRole>()
            : new HashSet<>(roles);

        boolean added = updatedRoles.add(role);
        return added ? toBuilder().roles(updatedRoles).build() : this;
    }

    /**
     * Update user with new password.
     *
     * @param newPasswordHash user new password
     *
     * @return user
     */
    public ConnectorUser changePassword(String newPasswordHash) {
        return this.toBuilder()
            .password(newPasswordHash)
            .mustChangePassword(false)
            .build();
    }

    /**
     * Force password change after first login.
     *
     * @return current user
     */
    public ConnectorUser forcePasswordChangeOnNextLogin() {
        return this.toBuilder()
            .mustChangePassword(true)
            .build();
    }

    /**
     * Retrieve current user roles name.
     *
     * @return user roles name
     */
    public Set<String> roleNames() {
        if (this.roles == null) {
            return Set.of();
        }

        return this.roles.stream()
            .map(ConnectorRole::name)
            .collect(Collectors.toUnmodifiableSet());
    }
}
