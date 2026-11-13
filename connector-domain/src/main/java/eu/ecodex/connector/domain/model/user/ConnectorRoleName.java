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

/**
 * Defines the various roles that can be assigned within the Connector system.
 *
 * <p>This enumeration represents the roles available in the system, which define
 * the level of access and permissions for users interacting with the Connector.
 * Roles may be used to enforce authorization policies and control user actions
 * within the system. Each constant in this enumeration corresponds to a specific
 * role and its associated privileges.
 *
 * <p>The enumeration is intended to facilitate role-based access control (RBAC)
 * mechanisms by providing a centralized definition of the supported roles.
 */
public enum ConnectorRoleName {
    ADMIN,
    USER,
    LOAD_TESTER;

    private static final String ROLE_PREFIX = "ROLE_";

    public String prefixedWithRole() {
        return ROLE_PREFIX + name();
    }
}
