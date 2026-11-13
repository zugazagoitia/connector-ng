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


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentDeletedException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorDownloadAttachmentsService")
class ConnectorDownloadAttachmentsServiceTest {
    private static final String INPUT_IDENTIFIER = "attachment-123";
    private static final String STORAGE_IDENTIFIER = "storage-ref-456";

    @Mock
    private ConnectorMessageAttachmentRepository attachmentRepository;
    @Mock
    private ConnectorFileStorageProvider storageProvider;

    @InjectMocks
    private ConnectorDownloadAttachmentService service;

    private static ConnectorMessageAttachment anAttachment() {
        return ConnectorMessageAttachment.builder()
                                         .identifier(ConnectorDownloadAttachmentsServiceTest.STORAGE_IDENTIFIER)
                                         .build();
    }

    @Nested
    @DisplayName("execute")
    class Execute {
        @Test
        void should_return_document_bytes_when_attachment_and_documentExist() {
            var attachment = anAttachment();
            var expected = "PDF-CONTENT".getBytes(StandardCharsets.UTF_8);
            when(attachmentRepository.findByIdentifier(INPUT_IDENTIFIER)).thenReturn(attachment);
            when(storageProvider.findByIdentifier(STORAGE_IDENTIFIER)).thenReturn(expected);

            var result = service.execute(INPUT_IDENTIFIER);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void should_query_storage_with_attachment_identifier_when_attachment_found() {
            var attachment = anAttachment();
            when(attachmentRepository.findByIdentifier(INPUT_IDENTIFIER)).thenReturn(attachment);
            when(storageProvider.findByIdentifier(STORAGE_IDENTIFIER)).thenReturn(new byte[]{1});

            service.execute(INPUT_IDENTIFIER);

            verify(storageProvider).findByIdentifier(STORAGE_IDENTIFIER);
        }

        @Test
        void should_throw_not_found_exception_when_attachment_not_found() {
            when(attachmentRepository.findByIdentifier(INPUT_IDENTIFIER)).thenReturn(null);

            assertThatThrownBy(() -> service.execute(INPUT_IDENTIFIER))
                .isInstanceOf(ConnectorMessageAttachmentNotFoundException.class);

            verifyNoInteractions(storageProvider);
        }

        @Test
        void should_throw_deletion_exception_when_attachment_has_been_deleted_from_storage() {
            var attachment = anAttachment().toBuilder()
                                           .deletedAt(Instant.now().minusSeconds(1000))
                                           .build();
            when(attachmentRepository.findByIdentifier(INPUT_IDENTIFIER)).thenReturn(attachment);

            assertThatThrownBy(() -> service.execute(INPUT_IDENTIFIER))
                .isInstanceOf(ConnectorMessageAttachmentDeletedException.class);

            verifyNoInteractions(storageProvider);
        }

        @Test
        void should_throw_attachment_exception_when_document_missing_from_storage() {
            var attachment = anAttachment();
            when(attachmentRepository.findByIdentifier(INPUT_IDENTIFIER)).thenReturn(attachment);
            when(storageProvider.findByIdentifier(STORAGE_IDENTIFIER)).thenReturn(null);

            assertThatThrownBy(() -> service.execute(INPUT_IDENTIFIER))
                .isInstanceOf(ConnectorMessageAttachmentException.class);
        }
    }
}
