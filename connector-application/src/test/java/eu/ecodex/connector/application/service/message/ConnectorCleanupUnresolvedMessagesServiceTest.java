/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessDomainIdentifierTestFixtures;
import eu.ecodex.connector.MessageContentTestFixtures;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.service.attachment.ConnectorAttachmentFilesDeleter;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
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
@DisplayName("ConnectorCleanupUnresolvedMessagesAttachmentsService")
class ConnectorCleanupUnresolvedMessagesServiceTest {
    private static final Instant THRESHOLD = Instant.parse("2026-10-23T22:00:00Z");

    @Mock
    private ConnectorMessageRepository messageRepository;
    @Mock
    private ConnectorAttachmentFilesDeleter filesDeleter;

    @InjectMocks
    private ConnectorCleanupUnresolvedMessagesService service;

    private ConnectorBusinessMessage message(String identifier) {
        return ConnectorBusinessMessage
            .builder()
            .businessDomainIdentifier(
                BusinessDomainIdentifierTestFixtures.createDefaultBusinessDomainIdentifier())
            .identifier(identifier)
            .businessContent(MessageContentTestFixtures.createContent())
            .build();
    }

    @Nested
    @DisplayName("execute")
    class Execute {
        @Nested
        @DisplayName("when there are no unresolved messages")
        class WhenThereAreNoUnresolvedMessages {

            @Test
            void should_not_call_the_deleter() {
                when(messageRepository.findAllUnresolved(THRESHOLD)).thenReturn(List.of());

                service.execute(THRESHOLD);

                verifyNoInteractions(filesDeleter);
            }

            @Test
            void should_not_delete_any_message() {
                when(messageRepository.findAllUnresolved(THRESHOLD)).thenReturn(List.of());

                service.execute(THRESHOLD);

                verify(messageRepository, never()).deleteByIdentifier(anyString());
            }
        }

        @Nested
        @DisplayName("when all file deletions succeed")
        class WhenAllFileDeletionsSucceed {
            @Test
            void should_delete_the_files_of_every_unresolved_message() {
                when(messageRepository.findAllUnresolved(THRESHOLD))
                    .thenReturn(List.of(message("msg-1"), message("msg-2")));
                when(filesDeleter.deleteActiveAttachmentsOf(anyString()))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(1, 0));

                service.execute(THRESHOLD);

                verify(filesDeleter).deleteActiveAttachmentsOf("msg-1");
                verify(filesDeleter).deleteActiveAttachmentsOf("msg-2");
            }

            @Test
            void should_delete_every_message() {
                when(messageRepository.findAllUnresolved(THRESHOLD))
                    .thenReturn(List.of(message("msg-1"), message("msg-2")));
                when(filesDeleter.deleteActiveAttachmentsOf(anyString()))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(1, 0));

                service.execute(THRESHOLD);

                verify(messageRepository).deleteByIdentifier("msg-1");
                verify(messageRepository).deleteByIdentifier("msg-2");
            }

            @Test
            void should_delete_the_message_when_it_has_no_active_attachment() {
                when(messageRepository.findAllUnresolved(THRESHOLD))
                    .thenReturn(List.of(message("msg-1")));
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(ConnectorAttachmentFilesDeleter.Result.NONE);

                service.execute(THRESHOLD);

                verify(messageRepository).deleteByIdentifier("msg-1");
            }

            @Test
            void should_delete_the_files_before_the_message() {
                when(messageRepository.findAllUnresolved(THRESHOLD))
                    .thenReturn(List.of(message("msg-1")));
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(1, 0));

                service.execute(THRESHOLD);

                var orders = inOrder(filesDeleter, messageRepository);
                orders.verify(filesDeleter).deleteActiveAttachmentsOf("msg-1");
                orders.verify(messageRepository).deleteByIdentifier("msg-1");
            }
        }

        @Nested
        @DisplayName("when a file deletion fails")
        class WhenAFileDeletionFails {
            @Test
            void should_not_delete_the_message_so_that_the_files_are_retried() {
                when(messageRepository.findAllUnresolved(THRESHOLD))
                    .thenReturn(List.of(message("msg-1")));
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(1, 1));

                service.execute(THRESHOLD);

                verify(messageRepository, never()).deleteByIdentifier(anyString());
            }

            @Test
            void should_still_delete_the_other_messages() {
                when(messageRepository.findAllUnresolved(THRESHOLD))
                    .thenReturn(List.of(message("msg-1"), message("msg-2")));
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(0, 1));
                when(filesDeleter.deleteActiveAttachmentsOf("msg-2"))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(1, 0));

                service.execute(THRESHOLD);

                verify(messageRepository, never()).deleteByIdentifier("msg-1");
                verify(messageRepository).deleteByIdentifier("msg-2");
            }
        }

        @Nested
        @DisplayName("when the cleanup of one message throws")
        class WhenTheCleanupOfOneMessageThrows {
            @Test
            void should_continue_with_the_next_message_when_the_files_deleter_throws() {
                when(messageRepository.findAllUnresolved(THRESHOLD))
                    .thenReturn(List.of(message("msg-1"), message("msg-2"), message("msg-3")));
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(ConnectorAttachmentFilesDeleter.Result.NONE);
                doThrow(new RuntimeException("database error"))
                    .when(filesDeleter).deleteActiveAttachmentsOf("msg-2");
                when(filesDeleter.deleteActiveAttachmentsOf("msg-3"))
                    .thenReturn(ConnectorAttachmentFilesDeleter.Result.NONE);

                assertThatCode(() -> service.execute(THRESHOLD)).doesNotThrowAnyException();

                verify(messageRepository, never()).deleteByIdentifier("msg-2");
                verify(messageRepository).deleteByIdentifier("msg-3");
            }

            @Test
            void should_continue_with_the_next_message_when_deleting_one_message_fails() {
                when(messageRepository.findAllUnresolved(THRESHOLD))
                    .thenReturn(List.of(message("msg-1"), message("msg-2")));
                when(filesDeleter.deleteActiveAttachmentsOf(anyString()))
                    .thenReturn(ConnectorAttachmentFilesDeleter.Result.NONE);
                doThrow(new RuntimeException("database error"))
                    .when(messageRepository).deleteByIdentifier("msg-1");

                assertThatCode(() -> service.execute(THRESHOLD)).doesNotThrowAnyException();

                verify(messageRepository).deleteByIdentifier("msg-2");
            }
        }

        @Nested
        @DisplayName("when the threshold is null")
        class WhenTheThresholdIsNull {
            @SuppressWarnings("DataFlowIssue")
            @Test
            void should_throw_and_do_nothing() {
                assertThatThrownBy(() -> service.execute(null))
                    .isInstanceOf(NullPointerException.class);

                verifyNoInteractions(messageRepository, filesDeleter);
            }
        }
    }
}
