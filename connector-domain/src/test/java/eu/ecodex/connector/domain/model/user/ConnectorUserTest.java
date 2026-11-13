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

import static eu.ecodex.connector.domain.model.user.ConnectorUser.DEFAULT_ADMIN_PASSWORD;
import static eu.ecodex.connector.domain.model.user.ConnectorUser.DEFAULT_ADMIN_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ConnectorUserTest {

    @Test
    void default_admin_user_should_have_default_username_and_password() {
        // Given
        // When
        var defaultAdminUser = ConnectorUser.defaultAdminUser();

        // Then
        assertThat(defaultAdminUser).isNotNull();
        assertThat(defaultAdminUser.username()).isEqualTo(DEFAULT_ADMIN_USER_NAME);
        assertThat(defaultAdminUser.password()).isEqualTo(DEFAULT_ADMIN_PASSWORD);
        assertThat(defaultAdminUser.enabled()).isTrue();
        assertThat(defaultAdminUser.mustChangePassword()).isTrue();
        assertThat(defaultAdminUser.roles()).isNotEmpty()
            .isEqualTo(Set.of(ConnectorRole.defaultAdminRole()));
    }

    @Test
    void is_default_admin_should_return_TRUE() {
        // Given
        var user = ConnectorUser.defaultAdminUser();

        // When
        boolean defaultAdmin = user.isDefaultAdmin();

        // Then
        assertThat(defaultAdmin).isTrue();
    }

    @Test
    void is_default_admin_should_return_FALSE() {
        // Given
        var user = ConnectorUser.builder().username("username").build();

        // When
        boolean defaultAdmin = user.isDefaultAdmin();

        // Then
        assertThat(defaultAdmin).isFalse();
    }


    @Test
    void edit_roles_should_add_new_roles() {
        // Given
        var roles = Set.of(ConnectorRole.defaultAdminRole(), ConnectorRole.defaultUserRole());
        var user = ConnectorUser.builder()
            .username("username")
            .build();

        // When
        var actual = user.editRoles(roles);

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.roles()).isEqualTo(roles);
    }

    @Test
    void edit_roles_should_replace_existing_with_new_roles() {
        // Given
        var roles = Set.of(ConnectorRole.defaultAdminRole(), ConnectorRole.defaultUserRole());
        var user = ConnectorUser.builder()
            .username("username")
            .roles(Set.of(ConnectorRole.defaultAdminRole()))
            .build();

        // When
        var actual = user.editRoles(roles);

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.roles()).isEqualTo(roles);
    }

    @Test
    void add_role_should_add_new_user_role_when_user_has_existing_role() {
        // Given
        var user = ConnectorUser.builder()
            .username("username")
            .roles(Set.of(ConnectorRole.defaultAdminRole()))
            .build();

        // When
        var actual = user.addRole(ConnectorRole.defaultUserRole());

        // Then
        assertThat(actual).isNotNull();
        var roles = Set.of(ConnectorRole.defaultAdminRole(), ConnectorRole.defaultUserRole());
        assertThat(actual.roles()).isEqualTo(roles);
    }

    @Test
    void add_role_should_add_new_user_role_when_user_has_already_new_role() {
        // Given
        var user = ConnectorUser.builder()
            .username("username")
            .roles(Set.of(ConnectorRole.defaultAdminRole()))
            .build();

        // When
        var actual = user.addRole(ConnectorRole.defaultAdminRole());

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.roles()).isEqualTo(Set.of(ConnectorRole.defaultAdminRole()));
    }

    @Test
    void add_role_should_add_new_user_role_when_user_has_no_role() {
        // Given
        var user = ConnectorUser.builder()
            .username("username")
            .build();

        // When
        var actual = user.addRole(ConnectorRole.defaultAdminRole());

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.roles()).isEqualTo(Set.of(ConnectorRole.defaultAdminRole()));
    }

    @Test
    void change_password_should_disable_must_change_password() {
        // Given
        var user = ConnectorUser.builder()
            .username("username")
            .mustChangePassword(true)
            .password("password")
            .roles(Set.of(ConnectorRole.defaultAdminRole()))
            .build();

        // When
        var actual = user.changePassword("newPasswordHash");

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.password()).isEqualTo("newPasswordHash");
        assertThat(actual.mustChangePassword()).isFalse();
    }

    @Test
    void force_password_change_on_next_login_should_enable_must_change_password() {
        // Given
        var user = ConnectorUser.builder()
            .username("username")
            .mustChangePassword(true)
            .password("password")
            .roles(Set.of(ConnectorRole.defaultAdminRole()))
            .build();

        // When
        var actual = user.forcePasswordChangeOnNextLogin();

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.mustChangePassword()).isTrue();
    }

    @Test
    void role_names_should_return_user_roles_name() {
        // Given
        var roles = Set.of(ConnectorRole.defaultAdminRole(), ConnectorRole.defaultUserRole());
        var user = ConnectorUser.builder()
            .username("username")
            .roles(roles)
            .build();

        // When
        var actual = user.roleNames();

        // Then
        assertThat(actual).isNotEmpty()
            .containsExactlyInAnyOrder(ConnectorRole.DEFAULT_ADMIN_ROLE,
                ConnectorRole.DEFAULT_USER_ROLE);
    }

    @Test
    void role_names_should_return_empty() {
        // Given
        var user = ConnectorUser.builder()
            .username("username")
            .build();

        // When
        var actual = user.roleNames();

        // Then
        assertThat(actual).isEmpty();
    }
}