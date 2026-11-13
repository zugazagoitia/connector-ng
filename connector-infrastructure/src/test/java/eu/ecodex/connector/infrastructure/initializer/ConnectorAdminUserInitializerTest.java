/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.initializer;

import static eu.ecodex.connector.domain.model.user.ConnectorRole.DEFAULT_ADMIN_ROLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.exception.user.ConnectorUserNotFoundException;
import eu.ecodex.connector.application.port.api.auth.role.ConnectorRegisterRole;
import eu.ecodex.connector.application.port.api.auth.role.ConnectorRetrieveRoleByName;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUserRoles;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRegisterUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRetrieveUserByUsername;
import eu.ecodex.connector.domain.model.user.ConnectorRole;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import eu.ecodex.connector.infrastructure.config.property.auth.ConnectorAdminUserProperties;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class ConnectorAdminUserInitializerTest {
    @Mock
    private ConnectorRegisterUser registerUser;
    @Mock
    private ConnectorEditUserRoles editUserRoles;
    @Mock
    private ConnectorRegisterRole registerRole;
    @Mock
    private ConnectorRetrieveUserByUsername retrieveUserByUsername;
    @Mock
    private ConnectorRetrieveRoleByName retrieveRoleByName;
    @Mock
    private ApplicationArguments applicationArguments;

    private ConnectorAdminUserProperties adminUserProperties;
    private ConnectorAdminUserInitializer initializer;

    private static ConnectorAdminUserProperties properties(
        String username, String pwd, String email
    ) {
        var props = new ConnectorAdminUserProperties();
        props.setEmail(email);
        props.setUsername(username);
        props.setPassword(pwd);
        return props;
    }

    @BeforeEach
    void setUp() {
        initializer = new ConnectorAdminUserInitializer(
            registerUser, registerRole, editUserRoles, retrieveUserByUsername,
            retrieveRoleByName, adminUserProperties
        );
    }

    @Test
    void run_should_register_admin_user_in_props_when_no_admin_user_exists() {
        // Given
        var username = "ADMIN";
        var adminRole = "ROLE_ADMIN";
        var pwd = "password";
        var email = "testadmin@email.com";
        adminUserProperties = properties(username, pwd, email);

        var role = ConnectorRole.builder().name(adminRole).build();
        var user = ConnectorUser.builder()
            .username(username)
            .password(pwd)
            .email(email)
            .roles(Set.of(role))
            .enabled(Boolean.TRUE)
            .mustChangePassword(Boolean.TRUE)
            .build();

        when(registerRole.execute(any())).thenReturn(role);
        when(registerUser.execute(any())).thenReturn(user);
        when(retrieveRoleByName.execute(any())).thenReturn(role);

        // When
        initializer = new ConnectorAdminUserInitializer(
            registerUser, registerRole, editUserRoles, retrieveUserByUsername,
            retrieveRoleByName, adminUserProperties
        );

        initializer.run(applicationArguments);

        // Then
        verify(registerRole).execute(role);
        verify(registerUser).execute(user);
        verify(retrieveRoleByName).execute(DEFAULT_ADMIN_ROLE);
        verifyNoMoreInteractions(registerRole, registerUser, retrieveUserByUsername,
            retrieveRoleByName, applicationArguments);
    }

    @Test
    void run_should_register_default_admin_when_no_user_in_props_and_no_admin_user_exists() {
        // Given
        var defaultAdmin = "admin";
        var roleAdmin = "ROLE_ADMIN";
        var defaultPwd = "123456";

        adminUserProperties = new ConnectorAdminUserProperties();

        var role = ConnectorRole.builder().name(roleAdmin).build();
        var user = ConnectorUser.builder()
            .username(defaultAdmin)
            .password(defaultPwd)
            .roles(Set.of(role))
            .enabled(Boolean.TRUE)
            .mustChangePassword(Boolean.TRUE)
            .build();

        when(registerRole.execute(any())).thenReturn(role);
        when(registerUser.execute(any())).thenReturn(user);
        when(retrieveUserByUsername.execute(any())).thenThrow(ConnectorUserNotFoundException.class);

        // When
        initializer = new ConnectorAdminUserInitializer(
            registerUser, registerRole, editUserRoles, retrieveUserByUsername,
            retrieveRoleByName, adminUserProperties
        );

        initializer.run(applicationArguments);

        // Then
        verify(registerRole).execute(role);
        verify(registerUser).execute(user);
        verify(retrieveUserByUsername).execute(defaultAdmin);

        verifyNoMoreInteractions(registerRole, registerUser, retrieveUserByUsername,
            retrieveRoleByName, applicationArguments);
    }

    @Test
    void run_should_do_nothing_when_no_user_in_props_and_admin_user_exists() {
        // Given
        var defaultAdmin = "admin";
        var roleAdmin = "ROLE_ADMIN";
        var defaultPwd = "123456";
        adminUserProperties = new ConnectorAdminUserProperties();
        var adminRole = ConnectorRole.builder().name(roleAdmin).build();
        var user = ConnectorUser.builder()
            .username(defaultAdmin)
            .password(defaultPwd)
            .roles(Set.of(adminRole))
            .enabled(Boolean.TRUE)
            .build();

        when(retrieveUserByUsername.execute(any())).thenReturn(user);

        // When
        initializer = new ConnectorAdminUserInitializer(
            registerUser, registerRole, editUserRoles, retrieveUserByUsername,
            retrieveRoleByName, adminUserProperties
        );

        initializer.run(applicationArguments);

        // Then
        verify(retrieveUserByUsername).execute(defaultAdmin);

        // verify init of default roles
        verify(registerRole).execute(adminRole); // init of default role
        var userRole = ConnectorRole.builder().name("ROLE_USER").build();
        var testRole = ConnectorRole.builder().name("ROLE_LOAD_TESTER").build();
        verify(registerRole).execute(userRole); // init of default role
        verify(registerRole).execute(testRole); // init of default role

        verifyNoMoreInteractions(registerRole, registerUser, retrieveUserByUsername,
            retrieveRoleByName, applicationArguments);
    }

    @Test
    void run_should_register_when_no_user_in_props_and_admin_user_exists_with_no_admin_role() {
        // Given
        var defaultAdmin = "admin";
        var roleAdmin = "ROLE_ADMIN";
        var defaultPwd = "123456";
        var identifier = "identifier";

        adminUserProperties = new ConnectorAdminUserProperties();

        var role = ConnectorRole.builder().name(roleAdmin).build();
        var user = ConnectorUser.builder()
            .uuid(identifier)
            .username(defaultAdmin)
            .password(defaultPwd)
            .enabled(Boolean.TRUE)
            .build();

        when(retrieveUserByUsername.execute(any())).thenReturn(user);
        when(registerRole.execute(any())).thenReturn(role);

        // When
        initializer = new ConnectorAdminUserInitializer(
            registerUser, registerRole, editUserRoles, retrieveUserByUsername,
            retrieveRoleByName, adminUserProperties
        );

        initializer.run(applicationArguments);

        // Then
        verify(retrieveUserByUsername).execute(defaultAdmin);
        verify(registerRole).execute(role);
        verify(editUserRoles).execute(identifier, Set.of(roleAdmin));

        verifyNoMoreInteractions(registerRole, registerUser, retrieveUserByUsername,
            retrieveRoleByName, applicationArguments);
    }
}
