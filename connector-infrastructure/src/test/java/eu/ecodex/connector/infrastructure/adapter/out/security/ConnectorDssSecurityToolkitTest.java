/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.security;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.ConnectorMessageDocumentTestFixtures;
import eu.ecodex.connector.FileTestFixtures;
import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.MessageContentTestFixtures;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.ConnectorSecurityToolkit;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.infrastructure.adapter.out.security.exception.ConnectorContainerException;
import eu.ecodex.connector.infrastructure.helper.ConnectorTempFilesHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@SuppressWarnings("DataFlowIssue")
public class ConnectorDssSecurityToolkitTest extends BaseContainerTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ConnectorSecurityToolkit securityToolkit;
    @MockitoBean
    private ConnectorFileStorageProvider fileStorageProvider;
    @MockitoBean
    private ConnectorTempFilesHelper tempFilesHelper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute((Connection con) -> {
            try (var statement = con.createStatement()) {
                for (String table : List.of(
                    "connector_message_attachments",
                    "connector_messages",
                    "connector_processing_modes",
                    "connector_business_domains"
                )) {
                    statement.execute("DELETE FROM %s WHERE id IS NOT NULL".formatted(table));
                }
            }
            return null;
        });
    }

    private ConnectorBusinessMessage createMessage() {
        return BusinessMessageTestFixtures
            .createOutboundMessage()
            .toBuilder()
            .businessContent(
                MessageContentTestFixtures
                    .createContent()
                    .toBuilder()
                    .xmlContent(MessageAttachmentTestFixtures.createBusinessContentAttachment())
                    .businessDocument(
                        ConnectorMessageDocumentTestFixtures
                            .createDocumentWithoutSignature()
                    )
                    .build()
            )
            .build();
    }

    private ConnectorBusinessMessage createMessageWithAttachment() {
        return createMessage()
            .toBuilder()
            .identifier("fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu")
            .attachments(
                List.of(
                    MessageAttachmentTestFixtures.createAttachment()
                )
            )
            .build();
    }

    private ConnectorBusinessMessage createMessageWithIdenticalAttachmentNames() {
        return createMessage()
            .toBuilder()
            .identifier("fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu")
            .attachments(
                List.of(
                    MessageAttachmentTestFixtures.createAttachment(),
                    MessageAttachmentTestFixtures.createAttachment(),
                    MessageAttachmentTestFixtures.createAttachment()
                )
            )
            .build();
    }

    @Nested
    @DisplayName("When building a container")
    class BuildContainer {
        @Test
        void should_build_container_when_message_has_no_attachments() throws IOException {
            when(fileStorageProvider.save(any(), (Path) any())).thenReturn(UUID.randomUUID().toString());
            when(fileStorageProvider.findByIdentifier(any()))
                .thenReturn(FileTestFixtures.readAsBytes("raw/document/NonSigned.pdf"));
            when(tempFilesHelper.create(anyString(), anyString()))
                .thenAnswer(invocation -> Files.createTempFile("connector", ".tmp"));

            var message = createMessage();

            var outputMessage = securityToolkit.buildContainer(message);

            assertThat(outputMessage).isNotNull();
        }

        @Test
        void should_build_container_when_message_has_an_attachment() throws IOException {
            when(fileStorageProvider.save(any(), (Path) any())).thenReturn(UUID.randomUUID().toString());
            when(fileStorageProvider.findByIdentifier(any()))
                .thenReturn(FileTestFixtures.readAsBytes("raw/document/NonSigned.pdf"))
                .thenReturn(FileTestFixtures.readAsBytes("raw/attachment/Attachment.png"));
            when(tempFilesHelper.create(anyString(), anyString()))
                .thenAnswer(invocation -> Files.createTempFile("connector", ".tmp"));

            var message = createMessageWithAttachment();

            var outputMessage = securityToolkit.buildContainer(message);

            assertThat(outputMessage).isNotNull();
        }
    }

    @Nested
    @DisplayName("When the container cannot be built")
    class BuildContainerFailures {
        @Test
        @Sql({
            "classpath:sql/business-domain.sql",
            "classpath:sql/processing-mode.sql",
            "classpath:sql/message.sql",
            "classpath:sql/attachment.sql"
        })
        void should_fail_when_message_has_duplicate_attachment_names() {
            var message = createMessageWithIdenticalAttachmentNames();

            assertThrows(
                ConnectorContainerException.class,
                () -> securityToolkit.buildContainer(message)
            );
        }

        @Test
        void should_fail_when_file_storage_provider_is_unavailable() {
            doThrow(RuntimeException.class).when(fileStorageProvider).save(any(), (Path) any());
            when(fileStorageProvider.findByIdentifier(any()))
                .thenReturn(FileTestFixtures.readAsBytes("raw/document/NonSigned.pdf"));

            var message = createMessage();

            assertThrows(
                ConnectorContainerException.class,
                () -> securityToolkit.buildContainer(message)
            );
        }

        @Test
        void should_fail_when_message_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> securityToolkit.buildContainer(null)
            );
        }
    }
}

