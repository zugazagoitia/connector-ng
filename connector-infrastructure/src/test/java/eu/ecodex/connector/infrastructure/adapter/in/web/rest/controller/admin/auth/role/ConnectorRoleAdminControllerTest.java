/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.auth.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.port.api.auth.role.ConnectorListRole;
import eu.ecodex.connector.domain.model.user.ConnectorRole;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.AbstractWebMvcTest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorRoleDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@WebMvcTest(ConnectorRoleAdminController.class)
class ConnectorRoleAdminControllerTest extends AbstractWebMvcTest {
    private static final String URL = "/api/v1/admin/users/roles";

    @MockitoBean
    ConnectorListRole listRole;

    @Autowired
    private RestTestClient apiClient;


    @Test
    void getAll_should_return_all_roles() {
        // Given
        var roleDto = ConnectorRoleDto.builder().name("new_role").build();
        var role = ConnectorRole.builder().name("new_role").build();

        when(listRole.execute()).thenReturn(List.of(role));

        // When
        var response = apiClient.get()
            .uri(URL)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(new ParameterizedTypeReference<List<ConnectorRoleDto>>() {
            });

        // Then
        var responseBody = response.getResponseBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).usingRecursiveComparison().isEqualTo(List.of(roleDto));

        verify(listRole).execute();
        verifyNoMoreInteractions(listRole);
    }

}
