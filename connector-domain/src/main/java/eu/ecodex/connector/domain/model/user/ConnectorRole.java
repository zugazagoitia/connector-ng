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
import java.time.Instant;
import lombok.Builder;

/**
 * Represents a role assigned to a user in the Connector system.
 *
 * <p>This record provides metadata about the role, including a unique identifier,the role's name,
 * and its creation and last updated timestamps.
 * It is an immutable data structure designed to store and share role-specific information across
 * the Connector system.
 *
 * <p>The class supports the builder pattern, offering a nested {@code Builder} class
 * that provides a flexible API for incrementally constructing instances of
 * {@code ConnectorUserRole}. Additionally, it includes a method to create a pre-populated builder
 * from an existing object.
 *
 * <p>This record is used as a field in other classes, such as {@code ConnectorUser},
 * to represent the roles associated with a user.
 */
@Builder(toBuilder = true)
public record ConnectorRole(String uuid,
                            @NotBlank String name,
                            Instant createdAt,
                            Instant updatedAt) {

    public static final String DEFAULT_ADMIN_ROLE = ConnectorRoleName.ADMIN.prefixedWithRole();
    public static final String DEFAULT_USER_ROLE = ConnectorRoleName.USER.prefixedWithRole();
    public static final String DEFAULT_LOAD_TESTER_ROLE =
        ConnectorRoleName.LOAD_TESTER.prefixedWithRole();

    /**
     * Create Default Administrator role.
     *
     * @return Default admin role.
     */
    public static ConnectorRole defaultAdminRole() {
        return ConnectorRole.builder().name(DEFAULT_ADMIN_ROLE).build();
    }

    public static ConnectorRole defaultUserRole() {
        return ConnectorRole.builder().name(DEFAULT_USER_ROLE).build();
    }

    public static ConnectorRole defaultLoadTesterRole() {
        return ConnectorRole.builder().name(DEFAULT_LOAD_TESTER_ROLE).build();
    }

    public boolean isDefaultAdminRole() {
        return DEFAULT_ADMIN_ROLE.equals(name);
    }
}
