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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConnectorRoleTest {

    @Test
    void is_default_admin_role_should_return_TRUE_when_user_role_is_admin_role() {
        // Given
        var role = ConnectorRole.defaultAdminRole();
        // When
        boolean isAdmin = role.isDefaultAdminRole();
        // Then
        assertThat(isAdmin).isTrue();
    }

    @Test
    void is_default_admin_role_should_return_FALSE_when_user_role_is_not_admin_role() {
        // Given
        var role = ConnectorRole.defaultUserRole();
        // When
        boolean isAdmin = role.isDefaultAdminRole();
        // Then
        assertThat(isAdmin).isFalse();
    }
}