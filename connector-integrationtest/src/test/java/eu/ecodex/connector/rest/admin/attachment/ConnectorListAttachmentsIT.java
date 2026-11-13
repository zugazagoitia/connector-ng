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

import static java.util.Map.entry;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.ecodex.connector.AbstractIntegrationTest;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.ConnectorAttachmentDto;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.util.UriComponentsBuilder;

@DisplayName("ConnectorListAttachmentsIT REST")
@Sql(
    statements = "DELETE FROM connector_business_domains WHERE id > 0",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class ConnectorListAttachmentsIT extends AbstractIntegrationTest {
    private static final String URL = "/api/v1/admin/attachments";
    private static final String MESSAGE_ID = "fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu";
    private static final Instant NOW = Instant.now();
    @Autowired
    private RestTestClient apiClient;

    static Stream<Arguments> attachmentFilters() {
        return Stream.of(
            arguments(new AttachmentFilter(null, null, null, null, null, null), 15),
            arguments(new AttachmentFilter(MESSAGE_ID, null, null, null, null, null), 4),
            arguments(new AttachmentFilter(MESSAGE_ID, "fake_file.pdf", null, null, null, null), 2),
            arguments(
                new AttachmentFilter(
                    MESSAGE_ID,
                    "fake_file.pdf",
                    "ATTACHMENT",
                    null,
                    NOW.minus(1, ChronoUnit.HOURS).toString(),
                    NOW.plus(1, ChronoUnit.HOURS).toString()
                ), 2
            ),
            arguments(
                new AttachmentFilter(
                    MESSAGE_ID,
                    "fake_file.pdf",
                    "ATTACHMENT",
                    "S3_BUCKET",
                    null,
                    null
                ),
                2
            ),
            arguments(
                new AttachmentFilter(
                    null,
                    "fake_file",
                    "ATTACHMENT",
                    "S3_BUCKET",
                    null,
                    null
                ), 4
            ),
            arguments(new AttachmentFilter(null, "fake_file", "ATTACHMENT", null, null, null), 4),
            arguments(new AttachmentFilter(null, "fake_file", null, null, null, null), 4),
            arguments(new AttachmentFilter(null, null, "ATTACHMENT", null, null, null), 4),
            arguments(new AttachmentFilter(null, null, null, "S3_BUCKET", null, null), 15)
        );
    }

    @AfterEach
    void cleanUp() {
        cleanDb();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("attachmentFilters")
    @WithAttachmentData
    void should_list_attachments(AttachmentFilter filter, int expectedSize) {
        var uri = UriComponentsBuilder
            .fromUriString(URL)
            .queryParamIfPresent(
                "messageIdentifier",
                Optional.ofNullable(filter.messageIdentifier())
            )
            .queryParamIfPresent("name", Optional.ofNullable(filter.filename()))
            .queryParamIfPresent("types", Optional.ofNullable(filter.type()))
            .queryParamIfPresent("storage", Optional.ofNullable(filter.storage()))
            .queryParamIfPresent("from", Optional.ofNullable(filter.from()))
            .queryParamIfPresent("to", Optional.ofNullable(filter.to()))
            .build()
            .toUri();

        apiClient.get()
                 .uri(uri)
                 .accept(MediaType.APPLICATION_JSON)
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<ConnectorPageResult<ConnectorAttachmentDto>>() {
                 })
                 .value(result -> assertThat(result)
                     .isNotNull()
                     .satisfies(r -> {
                         assertThat(r.content().size()).isEqualTo(expectedSize);
                         assertThat(r.size()).isEqualTo(expectedSize);
                         assertThat(r.totalElements()).isEqualTo(expectedSize);
                         assertThat(r.totalPages()).isEqualTo(1);
                     }));
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

    record AttachmentFilter(
        String messageIdentifier,
        String filename,
        String type,
        String storage,
        String from,
        String to
    ) {
        @Override
        public @NonNull String toString() {
            return Stream.of(
                             entry("messageIdentifier", messageIdentifier),
                             entry("name", filename),
                             entry("type", type),
                             entry("storage", storage),
                             entry("from", from),
                             entry("to", to)
                         )
                         .filter(e -> e.getValue() != null)
                         .map(e -> e.getKey() + "=" + e.getValue())
                         .collect(Collectors.joining(", ", "{", "}"));
        }
    }
}
