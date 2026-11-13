/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.rest.admin.role;

import static org.assertj.core.api.Assertions.assertThat;

import eu.ecodex.connector.AbstractIntegrationTest;
import eu.ecodex.connector.application.port.spi.auth.role.ConnectorRoleRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

class ConnectorRetrieveRoleIT extends AbstractIntegrationTest {
    public static final String PATH = "/api/v1/admin/users/roles";
    @Autowired
    private RestTestClient apiClient;

    @Autowired
    private ConnectorRoleRepository roleRepository;

    @AfterEach
    void cleanUp() {
        cleanDb();
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void getAll_should_succeeded_when_valid_token_is_provided() {
        var existing = roleRepository.findAll();
        assertThat(existing).isNotEmpty();

        var response = apiClient
            .get()
            .uri(PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(List.class)
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.size()).isEqualTo(existing.size());
    }
}
