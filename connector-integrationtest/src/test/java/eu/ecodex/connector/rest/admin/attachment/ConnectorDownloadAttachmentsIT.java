/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.rest.admin.attachment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.AbstractIntegrationTest;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.ErrorResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

@DisplayName("ConnectorDownloadAttachmentIT REST")
@Sql(
    statements = "DELETE FROM connector_business_domains WHERE id > 0",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
public class ConnectorDownloadAttachmentsIT extends AbstractIntegrationTest {
    @MockitoBean
    private ConnectorFileStorageProvider fileStorageProvider;

    @Autowired
    private RestTestClient apiClient;

    @AfterEach
    void cleanUp() {
        cleanDb();
    }

    @Test
    @WithAttachmentData
    void should_download_attachments() {
        when(fileStorageProvider.findByIdentifier(any())).thenReturn(new byte[] {1, 2, 3});

        var body = apiClient.get()
            .uri(
                "/api/v1/admin/attachments/d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file/download")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .exchange()
            .expectStatus().isOk()
            .expectBody(byte[].class)
            .returnResult()
            .getResponseBody();

        assertThat(body).isNotNull().isNotEmpty();
    }

    @Test
    @WithAttachmentData
    void should_failed_when_attachment_is_not_found() {
        apiClient.get()
            .uri("/api/v1/admin/attachments/unknown-id/download")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(ErrorResponse.class)
            .value(response -> {
                assert response != null;
                assertThat(response.status()).isEqualTo(404);
            });
    }

    @Test
    @WithAttachmentData
    void should_failed_when_is_deleted_from_the_storage() {
        apiClient.get()
                 .uri("/api/v1/admin/attachments/f07d1bfd-685a-4fee-b70d-a582cd0711c7_test_attachment/download")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().is4xxClientError()
                 .expectBody(ErrorResponse.class)
                 .value(response -> {
                     assert response != null;
                     assertThat(response.status()).isEqualTo(409);
                 });
    }

    @Test
    @WithAttachmentData
    void should_failed_when_attachment_is_no_longer_available_in_storage() {
        when(fileStorageProvider.findByIdentifier(any())).thenReturn(null);

        apiClient.get()
            .uri(
                "/api/v1/admin/attachments/d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file/download")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(ErrorResponse.class)
            .value(response -> {
                assert response != null;
                assertThat(response.status()).isEqualTo(409);
            });
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-service.sql",
        "classpath:sql/processing-mode-action.sql",
        "classpath:sql/message.sql",
        "classpath:sql/message-as4-properties.sql",
        "classpath:sql/attachment.sql",
        "classpath:sql/user.sql"
    })
    private @interface WithAttachmentData {
    }
}
