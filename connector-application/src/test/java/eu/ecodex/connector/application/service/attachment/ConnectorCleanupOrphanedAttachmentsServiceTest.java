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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorCleanupOrphanedAttachmentsService")
public class ConnectorCleanupOrphanedAttachmentsServiceTest {
    private static final Instant THRESHOLD = Instant.parse("2026-10-27T22:00:00Z");

    @Mock
    private ConnectorMessageAttachmentRepository attachmentRepository;
    @Mock
    private ConnectorAttachmentFilesDeleter filesDeleter;

    @InjectMocks
    private ConnectorCleanupOrphanedAttachmentsService service;

    @Nested
    @DisplayName("when there are no orphans")
    class NoOrphans {
        @Test
        void should_look_up_the_orphans_with_the_given_threshold() {
            when(attachmentRepository.findAllOrphaned(THRESHOLD)).thenReturn(List.of());

            service.execute(THRESHOLD);

            verify(attachmentRepository).findAllOrphaned(THRESHOLD);
        }

        @Test
        void should_not_call_the_deleter() {
            when(attachmentRepository.findAllOrphaned(THRESHOLD)).thenReturn(List.of());

            service.execute(THRESHOLD);

            verifyNoInteractions(filesDeleter);
        }
    }

    @Nested
    @DisplayName("when there are orphans")
    class WithOrphans {
        @Test
        void should_purge_the_orphans_with_the_deleter() {
            var orphans = List.of(
                mock(ConnectorMessageAttachment.class),
                mock(ConnectorMessageAttachment.class)
            );
            when(attachmentRepository.findAllOrphaned(THRESHOLD)).thenReturn(orphans);
            when(filesDeleter.purge(orphans))
                .thenReturn(new ConnectorAttachmentFilesDeleter.Result(2, 0));

            service.execute(THRESHOLD);

            verify(filesDeleter).purge(orphans);
        }

        @Test
        void should_complete_when_some_attachments_could_not_be_deleted() {
            var orphans = List.of(
                mock(ConnectorMessageAttachment.class),
                mock(ConnectorMessageAttachment.class)
            );
            when(attachmentRepository.findAllOrphaned(THRESHOLD)).thenReturn(orphans);
            when(filesDeleter.purge(orphans))
                .thenReturn(new ConnectorAttachmentFilesDeleter.Result(1, 1));

            service.execute(THRESHOLD);

            verify(filesDeleter).purge(orphans);
        }

        @Test
        void should_propagate_the_failure_of_the_deleter() {
            var orphans = List.of(mock(ConnectorMessageAttachment.class));
            when(attachmentRepository.findAllOrphaned(THRESHOLD)).thenReturn(orphans);
            when(filesDeleter.purge(orphans))
                .thenThrow(new IllegalStateException("storage unavailable"));

            assertThatThrownBy(() -> service.execute(THRESHOLD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("storage unavailable");
        }
    }

    @Nested
    @SuppressWarnings("DataFlowIssue")
    @DisplayName("when the input is invalid")
    class InvalidInput {
        @Test
        void should_throw_when_the_threshold_is_null() {
            assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void should_not_look_up_nor_delete_anything_when_the_threshold_is_null() {
            assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(attachmentRepository, filesDeleter);
        }
    }
}
