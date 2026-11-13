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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.ConnectorUserTestFixtures;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUserCommand;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorListUsers;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRegisterUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRemoveUser;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorRetrieveUserByIdentifier;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.AbstractWebMvcTest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@WebMvcTest(ConnectorUserAdminController.class)
class ConnectorEditUserAdminControllerTest extends AbstractWebMvcTest {
    private static final String URL = "/api/v1/admin/users";

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
    void should_edit_user_successfully() {
        // Given
        var connectorUser = ConnectorUserTestFixtures.createDefaultUser();
        var connectorUserRequest = ConnectorUserTestFixtures.createDefaultPatchUserRequest();
        var editUserCommand = ConnectorEditUserCommand.builder()
            .uuid(connectorUser.uuid())
            .email("test_user@email.com")
            .enabled(Boolean.TRUE)
            .build();

        when(connectorEditUser.execute(anyString(), any())).thenReturn(connectorUser);

        // When
        var response = apiClient.patch()
            .uri(URL + "/" + connectorUser.uuid())
            .contentType(MediaType.APPLICATION_JSON)
            .body(connectorUserRequest)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserDto.class);

        // Then
        var responseBody = response.getResponseBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
            .usingRecursiveComparison()
            .isEqualTo(ConnectorUserTestFixtures.createUserDto());

        var userArgumentCaptor = ArgumentCaptor.forClass(ConnectorEditUserCommand.class);
        verify(connectorEditUser).execute(eq(connectorUser.uuid()), userArgumentCaptor.capture());
        assertThat(userArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(
            editUserCommand
        );

        verifyNoMoreInteractions(connectorEditUser,
                                 connectorListUsers, connectorRemoveUser,
                                 connectorRegisterUser, connectorRetrieveUserByIdentifier);
    }
}
