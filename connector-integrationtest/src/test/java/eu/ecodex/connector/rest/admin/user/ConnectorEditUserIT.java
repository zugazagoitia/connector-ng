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
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditUserRequest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

class ConnectorEditUserIT extends AbstractIntegrationTest {
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
    void patch_should_succeeded_when_valid_credentials_are_provided() {
        var username = "test-user-it";
        var before = userRepository.findByUsername(username);
        assertThat(before).isNotEmpty();
        assertThat(before.get().enabled()).isTrue();

        var request = ConnectorEditUserRequest.builder()
            .enabled(false)
            .build();

        var registeredUser = apiClient.patch()
            .uri(StringUtils.joinWith("/", PATH, before.get().uuid()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ConnectorUserDto.class)
            .getResponseBody();

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.username()).isEqualTo(username);
        assertThat(registeredUser.enabled()).isFalse();

        var after = userRepository.findByUsername(username);
        assertThat(before).isNotEqualTo(after);
        assertThat(after).isNotEmpty();
        assertThat(after.get().enabled()).isFalse();
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void edit_should_failed_when_email_already_exists() {
        var username = "test-user2-it";
        var before = userRepository.findByUsername(username);
        assertThat(before).isNotEmpty();

        var request = ConnectorEditUserRequest.builder()
            .email("user-it@email.com")
            .enabled(true)
            .build();

        apiClient.patch()
            .uri(StringUtils.joinWith("/", PATH, before.get().uuid()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .is4xxClientError();

        var after = userRepository.findByUsername(username);
        assertThat(before).isEqualTo(after);
    }
}
