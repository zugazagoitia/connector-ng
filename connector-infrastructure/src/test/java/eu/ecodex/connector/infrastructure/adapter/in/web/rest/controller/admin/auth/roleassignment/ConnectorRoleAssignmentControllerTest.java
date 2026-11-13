/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.auth.roleassignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.ConnectorUserTestFixtures;
import eu.ecodex.connector.application.exception.role.ConnectorRoleNotFoundException;
import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUserRoles;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.AbstractWebMvcTest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditUserRolesRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@WebMvcTest(ConnectorRoleAssignmentController.class)
class ConnectorRoleAssignmentControllerTest extends AbstractWebMvcTest {
    private static final String URL = "/api/v1/admin/users/%s/roles";

    @MockitoBean
    ConnectorEditUserRoles connectorEditUserRoles;

    @Autowired
    private RestTestClient apiClient;


    @Test
    void edit_user_roles_should_return_OK() {
        // Given
        var userIdentifier = "user-identifier";
        var connectorUser = ConnectorUserTestFixtures.createDefaultUserWithRoles();
        var request = ConnectorUserTestFixtures.createEditUserRolesRequest();

        when(connectorEditUserRoles.execute(any(), anySet())).thenReturn(connectorUser);

        // When
        var registeredUser = apiClient.patch()
            .uri(String.format(URL, userIdentifier))
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        // Then
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.roles()).hasSize(1);

        verify(connectorEditUserRoles).execute(userIdentifier, Set.of("ROLE_ADMIN", "ROLE_USER"));
        verifyNoMoreInteractions(connectorEditUserRoles);
    }

    @Test
    void edit_user_roles_should_return_400_when_empty_request() {
        // Given
        var userIdentifier = "user-identifier";
        var request = ConnectorEditUserRolesRequest.builder().roles(Set.of()).build();


        // When
        apiClient.patch()
            .uri(String.format(URL, userIdentifier))
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Then
        verifyNoMoreInteractions(connectorEditUserRoles);
    }

    @Test
    void edit_user_roles_should_return_400_when_invalid_role() {
        // Given
        var userIdentifier = "user-identifier";
        var request = ConnectorUserTestFixtures.createInvalidEditUserRolesRequest();

        when(connectorEditUserRoles.execute(any(), anySet())).thenThrow(
            ConnectorRoleNotFoundException.class);

        // When
        apiClient.patch()
            .uri(String.format(URL, userIdentifier))
            .body(request)
            .exchange()
            .expectStatus()
            .isNotFound();

        // Then
        verify(connectorEditUserRoles).execute(userIdentifier, Set.of("INVALID_ROLE_ADMIN"));
        verifyNoMoreInteractions(connectorEditUserRoles);
    }
}
