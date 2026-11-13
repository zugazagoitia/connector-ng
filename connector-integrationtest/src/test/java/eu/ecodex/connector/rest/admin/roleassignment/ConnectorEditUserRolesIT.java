/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.rest.admin.roleassignment;

import static org.assertj.core.api.Assertions.assertThat;

import eu.ecodex.connector.AbstractIntegrationTest;
import eu.ecodex.connector.application.port.spi.auth.user.ConnectorUserRepository;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorCreateUserRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditUserRolesRequest;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

class ConnectorEditUserRolesIT extends AbstractIntegrationTest {
    public static final String PATH = "/api/v1/admin/users/%s/roles";
    @Autowired
    private RestTestClient apiClient;

    @Autowired
    private ConnectorUserRepository userRepository;

    @AfterEach
    void cleanUp() {
        cleanDb();
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void edit_user_roles_should_add_new_roles() {
        var username = "new_user_it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isEmpty();

        var request = ConnectorCreateUserRequest.builder()
            .username(username)
            .password("password")
            .email("test@email.com")
            .enabled(true)
            .build();

        var registeredUser = apiClient.post()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.roles()).isNotEmpty().containsExactly("ROLE_USER");

        existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();

        var editUserRolesRequest =
            ConnectorEditUserRolesRequest.builder().roles(Set.of("ROLE_USER_2")).build();
        registeredUser = apiClient.patch()
            .uri(String.format(PATH, registeredUser.uuid()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(editUserRolesRequest)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.roles()).hasSize(1)
            .containsExactly("ROLE_USER_2");
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void edit_user_role_should_replace_existing_user_roles() {
        var username = "test-user-it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();
        assertThat(existing.get().roles()).hasSize(2);

        var editUserRolesRequest =
            ConnectorEditUserRolesRequest.builder().roles(Set.of("ROLE_ADMIN")).build();

        var updated = apiClient.patch()
            .uri(String.format(PATH, existing.get().uuid()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(editUserRolesRequest)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.roles()).hasSize(1);
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void edit_roles_should_not_update_user_roles_when_nothing_to_update() {
        var username = "test-user-it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();
        assertThat(existing.get().roles()).hasSize(2);

        var editUserRolesRequest =
            ConnectorEditUserRolesRequest.builder().roles(
                Set.of("ROLE_TEST", "ROLE_USER_2")).build();

        var updated = apiClient.patch()
            .uri(String.format(PATH, existing.get().uuid()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(editUserRolesRequest)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.roles()).hasSize(2);
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void edit_user_roles_should_not_update_user_when_roles_does_not_exist() {
        var username = "test-user-it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();
        assertThat(existing.get().roles()).hasSize(2);

        var editUserRolesRequest =
            ConnectorEditUserRolesRequest.builder().roles(Set.of("ROLE_NEW_USER")).build();

        apiClient.patch()
            .uri(String.format(PATH, existing.get().uuid()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(editUserRolesRequest)
            .exchange()
            .expectStatus()
            .isNotFound();

        existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();
        assertThat(existing.get().roles()).hasSize(2);
    }
}
