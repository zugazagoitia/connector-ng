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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.exception.role.ConnectorRoleNotFoundException;
import eu.ecodex.connector.application.exception.user.ConnectorUserNotFoundException;
import eu.ecodex.connector.application.port.api.auth.role.ConnectorRetrieveRoleByName;
import eu.ecodex.connector.application.port.api.auth.role.ConnectorRetrieveRolesByNames;
import eu.ecodex.connector.application.port.spi.auth.user.ConnectorUserRepository;
import eu.ecodex.connector.domain.model.user.ConnectorRole;
import eu.ecodex.connector.domain.model.user.ConnectorRoleName;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorEditUserRolesServiceTest {
    @Mock
    private ConnectorRetrieveRolesByNames retrieveRolesByNames;
    @Mock
    private ConnectorRetrieveRoleByName retrieveRoleByName;

    @Mock
    private ConnectorUserRepository userRepository;

    @InjectMocks
    private ConnectorEditUserRolesService editUserRolesService;

    @Test
    void edit_user_roles_should_throw_exception_when_user_not_found() {
        // Given
        var userId = "uuid";
        var role = "ROLE_ADMIN";
        var requestedNames = Set.of(role);

        // When
        when(userRepository.findByUuid(userId)).thenReturn(Optional.empty());

        assertThrows(ConnectorUserNotFoundException.class,
            () -> editUserRolesService.execute(userId, requestedNames));

        assertNoMoreInteractions();
    }

    @Test
    void edit_user_roles_should_throw_exception_when_default_user_role_not_found() {
        // Given
        var userId = "uuid";
        var user = ConnectorUser.builder()
            .uuid(userId)
            .build();

        // When
        when(userRepository.findByUuid(userId)).thenReturn(Optional.of(user));
        when(retrieveRoleByName.execute(any())).thenThrow(ConnectorRoleNotFoundException.class);

        // Then
        assertThrows(ConnectorRoleNotFoundException.class,
            () -> editUserRolesService.execute(userId, Set.of()));

        verify(retrieveRoleByName).execute(ConnectorRoleName.USER.prefixedWithRole());
        assertNoMoreInteractions();
    }

    @Test
    void edit_user_roles_should_do_nothing_when_roles_are_already_assigned() {
        // Given
        var userId = "uuid";
        var requestedNames = Set.of("ROLE_USER", "ROLE_ADMIN");

        var roleUser = ConnectorRole.builder().name("ROLE_USER").build();
        var roleAdmin = ConnectorRole.builder().name("ROLE_ADMIN").build();
        var user = ConnectorUser.builder()
            .uuid(userId)
            .roles(new HashSet<>(Set.of(roleUser, roleAdmin)))
            .build();

        when(userRepository.findByUuid(userId)).thenReturn(Optional.of(user));

        // When
        var actual = editUserRolesService.execute(userId, requestedNames);

        // Then
        assertThat(actual).isNotNull().isEqualTo(user);
        verify(userRepository).findByUuid(userId);

        assertNoMoreInteractions();
    }

    @Test
    void edit_user_roles_should_add_role_to_user_when_user_has_no_role() {
        // Given
        var userId = "uuid";
        var roleName = "ROLE_USER";
        var requestedNames = Set.of(roleName);
        var roleUser = ConnectorRole.builder().name(roleName).build();
        var user = ConnectorUser.builder()
            .uuid(userId)
            .build();

        var expected = user.toBuilder()
            .roles(Set.of(roleUser))
            .build();
        when(userRepository.findByUuid(userId)).thenReturn(Optional.of(user));
        when(retrieveRolesByNames.execute(anySet())).thenReturn(Set.of(roleUser));
        when(userRepository.save(any())).thenReturn(expected);

        // When
        var actual = editUserRolesService.execute(userId, requestedNames);

        // Then
        assertThat(actual).isNotNull();

        verify(userRepository).findByUuid(userId);
        verify(retrieveRolesByNames).execute(requestedNames);
        verify(userRepository).save(expected);

        assertNoMoreInteractions();
    }

    @Test
    void edit_user_roles_should_replace_user_existing_role_with_new_role() {
        // Given
        var userId = "uuid";
        var roleName = "ROLE_USER";
        var requestedNames = Set.of(roleName);
        var roleUser = ConnectorRole.builder().name(roleName).build();
        var roleAdmin = ConnectorRole.builder().name("ROLE_ADMIN").build();
        var user = ConnectorUser.builder()
            .uuid(userId)
            .roles(new HashSet<>(Collections.singleton(roleAdmin)))
            .build();

        var expected = user.toBuilder()
            .roles(Set.of(roleUser))
            .build();

        // When
        when(userRepository.findByUuid(userId)).thenReturn(Optional.of(user));
        when(retrieveRolesByNames.execute(anySet())).thenReturn(Set.of(roleUser));
        when(userRepository.save(any())).thenReturn(expected);

        var actual = editUserRolesService.execute(userId, requestedNames);

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.roles()).usingRecursiveComparison().isEqualTo(Set.of(roleUser));
        verify(userRepository).findByUuid(userId);
        verify(retrieveRolesByNames).execute(requestedNames);
        verify(userRepository).save(expected);

        assertNoMoreInteractions();
    }

    @Test
    void edit_user_roles_should_add_default_role_to_user_and_throws_exception() {
        // Given
        var userId = "uuid";
        var roleName = "INVALID_ROLE_USER";

        var roleUser = ConnectorRole.builder().name(roleName).build();
        var user = ConnectorUser.builder()
            .uuid(userId)
            .build();

        var expected = user.toBuilder()
            .roles(Set.of(roleUser))
            .build();

        // When
        when(userRepository.findByUuid(userId)).thenReturn(Optional.of(user));
        when(retrieveRolesByNames.execute(anySet())).thenReturn(Set.of());
        when(retrieveRoleByName.execute(any())).thenReturn(roleUser);
        when(userRepository.save(any())).thenReturn(expected);

        var requestedNames = Set.of(roleName);
        assertThrows(ConnectorRoleNotFoundException.class,
            () -> editUserRolesService.execute(userId, requestedNames));

        // Then
        verify(userRepository).findByUuid(userId);
        verify(retrieveRolesByNames).execute(requestedNames);
        verify(retrieveRoleByName).execute(ConnectorRoleName.USER.prefixedWithRole());
        verify(userRepository).save(expected);

        assertNoMoreInteractions();
    }

    @Test
    void edit_user_roles_should_add_default_user_role_to_user() {
        // Given
        var userId = "uuid";
        var roleName = "ROLE_USER";
        var roleUser = ConnectorRole.builder().name(roleName).build();
        var user = ConnectorUser.builder()
            .uuid(userId)
            .build();

        var expected = user.toBuilder()
            .roles(Set.of(roleUser))
            .build();

        // When
        when(userRepository.findByUuid(userId)).thenReturn(Optional.of(user));
        when(retrieveRoleByName.execute(any())).thenReturn(roleUser);
        when(userRepository.save(any())).thenReturn(expected);

        var actual = editUserRolesService.execute(userId, Set.of());

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.roles()).usingRecursiveComparison().isEqualTo(Set.of(roleUser));
        verify(userRepository).findByUuid(userId);
        verify(retrieveRoleByName).execute(ConnectorRoleName.USER.prefixedWithRole());
        verify(userRepository).save(expected);

        assertNoMoreInteractions();
    }

    private void assertNoMoreInteractions() {
        verifyNoMoreInteractions(retrieveRolesByNames, retrieveRoleByName, userRepository);
    }
}
