/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.rest.admin.user;

import static org.assertj.core.api.Assertions.assertThat;

import eu.ecodex.connector.AbstractIntegrationTest;
import eu.ecodex.connector.application.port.spi.auth.user.ConnectorUserRepository;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorCreateUserRequest;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

class ConnectorRegisterUserIT extends AbstractIntegrationTest {
    public static final String PATH = "/api/v1/admin/users";
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
    void register_should_succeeded_and_create_user_default_role_when_no_roles_provided() {
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
            .uri(PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(registeredUser).isNotNull();

        existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();
        assertThat(existing.get().roles()).hasSize(1);
        assertThat(existing.get().roleNames()).containsExactly("ROLE_USER");
        assertThat(existing.get().mustChangePassword()).isTrue();
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void register_should_succeeded_when_valid_credentials_are_provided() {
        var username = "new_user_it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isEmpty();

        var request = ConnectorCreateUserRequest.builder()
            .username(username)
            .password("password")
            .email("test@email.com")
            .roles(Set.of("ROLE_ADMIN"))
            .enabled(true)
            .build();

        var registeredUser = apiClient.post()
            .uri(PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(registeredUser).isNotNull();

        existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();
        assertThat(existing.get().roles()).hasSize(1);
        assertThat(existing.get().mustChangePassword()).isTrue();
    }

    @Test
    void register_should_failed_when_invalid_token() {
        var username = "new_user_it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isEmpty();

        var request = ConnectorCreateUserRequest
            .builder()
            .username(username)
            .password("password")
            .email("test@email.com")
            .enabled(true)
            .build();

        apiClient.post()
            .uri(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isUnauthorized();

        existing = userRepository.findByUsername(username);
        assertThat(existing).isEmpty();
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void register_should_failed_when_user_already_exists() {
        var username = "test-user-it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();

        var request = ConnectorCreateUserRequest
            .builder()
            .username(username)
            .password("password")
            .email("test@email.com")
            .enabled(true)
            .build();

        apiClient.post()
            .uri(PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .is4xxClientError();

        existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void register_should_failed_when_missing_a_mandatory_field() {
        var username = "new_user_it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isEmpty();

        var request = ConnectorCreateUserRequest
            .builder()
            .username(username)
            .password(StringUtils.EMPTY)
            .enabled(true)
            .build();

        apiClient.post()
            .uri(PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest();

        existing = userRepository.findByUsername(username);
        assertThat(existing).isEmpty();
    }
}
