/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.attachment;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentException;
import eu.ecodex.connector.application.port.api.attachment.FileUploadCommand;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("DataFlowIssue")

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorUploadAttachmentsService")
public class ConnectorUploadAttachmentsServiceTest {
    @Mock
    private ConnectorMessageAttachmentRepository attachmentRepository;
    @Mock
    private ConnectorFileStorageProvider storageProvider;

    @InjectMocks
    private ConnectorUploadAttachmentsService uploadAttachmentsService;

    private Path provideTemporaryPath() throws IOException {
        var tempPath = Files.createTempFile("test-upload-", ".txt");
        Files.writeString(tempPath, "test content", StandardOpenOption.WRITE);

        return tempPath;
    }

    @Nested
    @DisplayName("when the upload succeeds")
    class WhenUploadSucceeds {
        @Test
        void should_upload_the_attachments() throws IOException {
            var savedAttachment = MessageAttachmentTestFixtures.createdAttachment();
            when(attachmentRepository.save(any())).thenReturn(savedAttachment);
            when(storageProvider.save(
                any(),
                (Path) any()
            )).thenReturn(savedAttachment.identifier());

            var fileUploadCommand = new FileUploadCommand(
                "test_attachment.txt",
                100L,
                "text/plain",
                provideTemporaryPath(),
                "test_message"
            );

            var attachments = uploadAttachmentsService.execute(List.of(fileUploadCommand));

            assertThat(attachments).isNotNull();
            assertThat(attachments).hasSize(1);

            var attachment = attachments.getFirst();
            assertThat(attachment.identifier()).endsWith("test_attachment");
            assertThat(attachment.name()).isEqualTo("test_attachment.txt");
            assertThat(attachment.contentType()).isEqualTo("text/plain");
            assertThat(attachment.size()).isEqualTo(100L);
            assertThat(attachment.createdAt()).isNotNull();
            assertThat(attachment.updatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("when the upload fails")
    class WhenUploadFails {
        @Test
        void should_fail_when_the_storage_provider_fails() throws IOException {
            var savedAttachment = MessageAttachmentTestFixtures.createAttachment();
            when(attachmentRepository.save(any())).thenReturn(savedAttachment);
            doThrow(RuntimeException.class).when(storageProvider).save(any(), (Path) any());

            var tempFile = provideTemporaryPath();
            var commands = List.of(new FileUploadCommand(
                "test_attachment.txt",
                100L,
                "text/plain",
                tempFile,
                "test_message"
            ));

            assertThrows(
                ConnectorMessageAttachmentException.class,
                () -> uploadAttachmentsService.execute(commands)
            );
        }

        @Test
        void should_fail_when_the_upload_commands_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> uploadAttachmentsService.execute(null)
            );
        }
    }
}
