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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorAttachmentVerifierServiceTest {

    @Mock
    private ConnectorMessageAttachmentRepository attachmentRepository;

    @InjectMocks
    private ConnectorAttachmentVerifierService attachmentVerifierService;

    @Test
    void should_succeeded() {
        // Given
        var attachment = ConnectorMessageAttachment.builder()
            .identifier("identifier")
            .build();
        var attachment2 = ConnectorMessageAttachment.builder()
            .identifier("identifier2")
            .build();
        var attachments = List.of(
            attachment, attachment2
        );

        when(attachmentRepository.existsByIdentifier(any())).thenReturn(true);
        // When
        attachmentVerifierService.execute(attachments);

        // Then
        verify(attachmentRepository).existsByIdentifier("identifier");
        verify(attachmentRepository).existsByIdentifier("identifier2");
        verifyNoMoreInteractions(attachmentRepository);
    }

    @Test
    void should_succeeded_when_attachments_is_empty() {
        // Given
        // When
        attachmentVerifierService.execute(List.of());

        // Then
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    void should_failed_when_not_exists() {
        // Given
        var attachment = ConnectorMessageAttachment.builder()
            .identifier("identifier")
            .build();
        var attachment2 = ConnectorMessageAttachment.builder()
            .identifier("identifier2")
            .build();
        var attachments = List.of(
            attachment, attachment2
        );

        when(attachmentRepository.existsByIdentifier(anyString())).thenReturn(false);
        // When
        assertThrows(ConnectorMessageAttachmentNotFoundException.class,
            () -> attachmentVerifierService.execute(attachments));

        // Then
        verify(attachmentRepository).existsByIdentifier("identifier");
        verifyNoMoreInteractions(attachmentRepository);
    }

    @Test
    void should_failed_when_duplicate_attachment_identifier() {
        // Given
        var attachment = ConnectorMessageAttachment.builder()
            .identifier("identifier")
            .build();
        var attachment2 = ConnectorMessageAttachment.builder()
            .identifier("identifier")
            .build();
        var attachments = List.of(
            attachment, attachment2
        );

        when(attachmentRepository.existsByIdentifier(anyString())).thenReturn(true);
        // When
        assertThrows(ConnectorMessageAttachmentException.class,
            () -> attachmentVerifierService.execute(attachments));

        // Then
        verify(attachmentRepository).existsByIdentifier("identifier");
        verifyNoMoreInteractions(attachmentRepository);
    }
}
