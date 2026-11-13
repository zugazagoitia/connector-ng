/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.rest.admin.evidence;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import eu.ecodex.connector.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

@DisplayName("ConnectorRetrieveEvidenceIT REST")
@Sql(
    statements = "DELETE FROM connector_business_domains WHERE id > 0",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
public class ConnectorRetrieveEvidenceIT extends AbstractIntegrationTest {
    private static final String URL = "/api/v1/admin/evidences/%s/download";
    @Autowired
    private RestTestClient apiClient;

    @AfterEach
    void cleanUp() {
        cleanDb();
    }

    @Test
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-service.sql",
        "classpath:sql/processing-mode-action.sql",
        "classpath:sql/message.sql",
        "classpath:sql/message-as4-properties.sql",
        "classpath:sql/evidence.sql",
        "classpath:sql/user.sql"
    })
    void should_export_connector_messages_report() {
        byte[] body = apiClient.get()
                               .uri(uriBuilder -> uriBuilder
                                   .path(URL.formatted("f6cb9e83-4283-4255-8bbf-9cb0920fc1ef"))
                                   .build())
                               .header(
                                   HttpHeaders.AUTHORIZATION,
                                   "Bearer " + generateDefaultAdminToken()
                               )
                               .exchange()
                               .expectStatus().isOk()
                               .expectHeader().contentType(MediaType.APPLICATION_XML)
                               .expectBody(byte[].class)
                               .returnResult()
                               .getResponseBody();

        assertThat(body).isNotNull().isNotEmpty();
    }

    @Test
    @Sql({
        "classpath:sql/user.sql"
    })
    void should_return_404_when_evidence_does_not_exist() {
        apiClient.get()
                 .uri(uriBuilder -> uriBuilder
                     .path(URL.formatted("f6cb9e83-4283-4255-8bbf-9cb0920fc1ef"))
                     .build())
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isNotFound();
    }
}
