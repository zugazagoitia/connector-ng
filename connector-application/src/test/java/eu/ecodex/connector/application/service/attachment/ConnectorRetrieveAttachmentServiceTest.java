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
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorRetrieveAttachmentService")
class ConnectorRetrieveAttachmentServiceTest {
    private static final String IDENTIFIER = "attachment-123";

    @Mock
    private ConnectorMessageAttachmentRepository attachmentRepository;

    @InjectMocks
    private ConnectorRetrieveAttachmentService service;

    private static ConnectorMessageAttachment anAttachment() {
        return ConnectorMessageAttachment.builder()
                                         .identifier(ConnectorRetrieveAttachmentServiceTest.IDENTIFIER)
                                         .build();
    }

    @Nested
    @DisplayName("execute")
    class Execute {
        @Test
        void should_return_attachment_when_found() {
            var attachment = anAttachment();
            when(attachmentRepository.findByIdentifier(IDENTIFIER)).thenReturn(attachment);

            var result = service.execute(IDENTIFIER);

            assertThat(result).isSameAs(attachment);
        }

        @Test
        void should_throw_not_found_exception_when_attachment_not_found() {
            when(attachmentRepository.findByIdentifier(IDENTIFIER)).thenReturn(null);

            assertThatThrownBy(() -> service.execute(IDENTIFIER))
                .isInstanceOf(ConnectorMessageAttachmentNotFoundException.class)
                .hasMessage("Attachment not found");
        }
    }
}
