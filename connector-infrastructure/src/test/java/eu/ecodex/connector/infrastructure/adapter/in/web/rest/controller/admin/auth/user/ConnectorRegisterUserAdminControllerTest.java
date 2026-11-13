/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.auth.user;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.ConnectorUserTestFixtures;
import eu.ecodex.connector.application.exception.user.ConnectorUserAlreadyExistsException;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorListUsers;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRegisterUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRemoveUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRetrieveUserByIdentifier;
import eu.ecodex.connector.application.port.spi.auth.login.ConnectorUserPasswordEncoder;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.AbstractWebMvcTest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorCreateUserRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@WebMvcTest(ConnectorUserAdminController.class)
class ConnectorRegisterUserAdminControllerTest extends AbstractWebMvcTest {
    private static final String URL = "/api/v1/admin/users";

    @MockitoBean
    ConnectorUserPasswordEncoder passwordEncoder;
    @MockitoBean
    private ConnectorRetrieveUserByIdentifier connectorRetrieveUserByIdentifier;
    @MockitoBean
    private ConnectorRegisterUser connectorRegisterUser;
    @MockitoBean
    private ConnectorEditUser connectorEditUser;
    @MockitoBean
    private ConnectorRemoveUser connectorRemoveUser;
    @MockitoBean
    private ConnectorListUsers connectorListUsers;

    @Autowired
    private RestTestClient apiClient;

    @Test
    void should_register_user_without_roles_successfully() {
        // Given
        var connectorUser = ConnectorUserTestFixtures.createDefaultUser();
        var connectorUserRequest = ConnectorUserTestFixtures.createDefaultUserRequest();
        when(connectorRegisterUser.execute(any())).thenReturn(connectorUser);

        // When
        var response = apiClient.post()
            .uri(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(connectorUserRequest)
            .exchange()
            .expectStatus()
            .isCreated()
            .returnResult(ConnectorUserDto.class);

        // Then
        var responseBody = response.getResponseBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
            .usingRecursiveComparison()
            .isEqualTo(ConnectorUserTestFixtures.createUserDto());

        var userArgumentCaptor = ArgumentCaptor.forClass(ConnectorUser.class);
        verify(connectorRegisterUser).execute(userArgumentCaptor.capture());
        assertThat(userArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(
            ConnectorCreateUserRequest.toDomain(connectorUserRequest)
        );
        assertNoMoreInteractions();
    }

    @Test
    void should_register_user_with_user_roles_successfully() {
        // Given
        var connectorUser = ConnectorUserTestFixtures.createDefaultUserWithRoles();
        var connectorUserRequest = ConnectorUserTestFixtures.createDefaultUserRequestWithRoles();
        when(connectorRegisterUser.execute(any())).thenReturn(connectorUser);

        // When
        var response = apiClient.post()
            .uri(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(connectorUserRequest)
            .exchange()
            .expectStatus()
            .isCreated()
            .returnResult(ConnectorUserDto.class);

        // Then
        var responseBody = response.getResponseBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
            .usingRecursiveComparison()
            .isEqualTo(ConnectorUserTestFixtures.createUserDtoWithRoles());

        var userArgumentCaptor = ArgumentCaptor.forClass(ConnectorUser.class);
        verify(connectorRegisterUser).execute(userArgumentCaptor.capture());
        assertThat(userArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(
            ConnectorCreateUserRequest.toDomain(connectorUserRequest)
        );
        assertNoMoreInteractions();
    }

    @Test
    void should_not_register_user_when_username_is_missing() {
        // Given
        var connectorUserRequest = ConnectorUserTestFixtures.createUserRequest(
            "", null, "password"
        );

        // When
        apiClient.post()
            .uri(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(connectorUserRequest)
            .exchange()
            .expectStatus()
            .is4xxClientError();

        // Then
        assertNoMoreInteractions();
    }

    @Test
    void should_not_register_user_when_already_exists() {
        // Given
        var connectorUserRequest = ConnectorUserTestFixtures.createDefaultUserRequest();
        when(connectorRegisterUser.execute(any())).thenThrow(
            new ConnectorUserAlreadyExistsException("msg"));

        // When
        var response = apiClient.post()
            .uri(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(connectorUserRequest)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT);

        // Then
        assertThat(response).isNotNull();
        var userArgumentCaptor = ArgumentCaptor.forClass(ConnectorUser.class);
        verify(connectorRegisterUser).execute(userArgumentCaptor.capture());
        assertThat(userArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(
            ConnectorCreateUserRequest.toDomain(connectorUserRequest)
        );
        assertNoMoreInteractions();
    }

    private void assertNoMoreInteractions() {
        verifyNoMoreInteractions(connectorEditUser,
                                 connectorListUsers, connectorRemoveUser,
                                 connectorRegisterUser, connectorRetrieveUserByIdentifier);
    }
}
