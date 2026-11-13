/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.rest.auth.user;

import static eu.ecodex.connector.domain.model.user.ConnectorUser.DEFAULT_ADMIN_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import eu.ecodex.connector.AbstractIntegrationTest;
import eu.ecodex.connector.application.port.spi.auth.user.ConnectorUserRepository;
import eu.ecodex.connector.domain.model.auth.ConnectorUserAuthenticationResult;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.login.ConnectorLoginRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorCreateUserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

class ConnectorRetrieveSelfIT extends AbstractIntegrationTest {
    public static final String PATH = "/api/v1/auth/me";
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
    void get_me_on_admin_should_succeeded_when_valid_token_is_provided() {
        var username = DEFAULT_ADMIN_USER_NAME;
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();

        var response = apiClient.get()
            .uri(PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo(username);
        assertThat(response.enabled()).isEqualTo(existing.get().enabled());
        assertThat(response.roles().size()).isEqualTo(existing.get().roles().size());
        assertThat(response.uuid()).isEqualTo(existing.get().uuid());
        assertThat(response.email()).isEqualTo(existing.get().email());
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void get_me_should_succeeded_when_valid_token_is_provided() {

        var username = "new_user_it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isEmpty();

        // register user
        var request = ConnectorCreateUserRequest
            .builder()
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
        existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();

        // login user
        var loginRequest = ConnectorLoginRequest
            .builder()
            .username(username)
            .password("password")
            .build();

        var loginResponse = apiClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginRequest)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserAuthenticationResult.class)
            .getResponseBody();

        assertThat(loginResponse).isNotNull();
        var accessToken = loginResponse.accessToken();
        assertThat(accessToken).isNotBlank();


        var response = apiClient.get()
            .uri(PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo(username);
        assertThat(response.enabled()).isEqualTo(existing.get().enabled());
        assertThat(response.roles().size()).isEqualTo(existing.get().roles().size());
        assertThat(response.uuid()).isEqualTo(existing.get().uuid());
        assertThat(response.email()).isEqualTo(existing.get().email());
    }


    @Test
    @Sql({"classpath:sql/user.sql"})
    void get_me_should_failed_when_invalid_token_is_provided() {
        var username = "test-user-it";
        var existing = userRepository.findByUsername(username);
        assertThat(existing).isNotEmpty();

        apiClient.get()
            .uri(PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ")
            .exchange()
            .expectStatus()
            .isUnauthorized();

    }
}
