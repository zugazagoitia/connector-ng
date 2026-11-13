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
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.exception.attachment.ConnectorAttachmentCleanupException;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.service.attachment.ConnectorAttachmentFilesDeleter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorCleanupResolvedMessageAttachmentsService")
class ConnectorCleanupResolvedMessageAttachmentsServiceTest {
    @Mock
    private ConnectorAttachmentFilesDeleter filesDeleter;
    @Mock
    private ConnectorMessageRepository messageRepository;

    @InjectMocks
    private ConnectorCleanupResolvedMessageAttachmentsService service;

    @Nested
    @DisplayName("execute")
    @SuppressWarnings("DataFlowIssue")
    class Execute {
        @Nested
        @DisplayName("when all deletions succeed")
        class WhenAllDeletionsSucceed {
            @Test
            void should_delete_the_attachments_of_the_message() {
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(2, 0));

                assertThatCode(() -> service.execute("msg-1")).doesNotThrowAnyException();

                verify(filesDeleter).deleteActiveAttachmentsOf("msg-1");
            }

            @Test
            void should_mark_the_message_as_deleted() {
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(2, 0));

                service.execute("msg-1");

                verify(messageRepository).setAsDeleted("msg-1");
            }

            @Test
            void should_mark_the_message_as_deleted_when_it_has_no_active_attachment() {
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(ConnectorAttachmentFilesDeleter.Result.NONE);

                assertThatCode(() -> service.execute("msg-1")).doesNotThrowAnyException();

                verify(messageRepository).setAsDeleted("msg-1");
            }
        }

        @Nested
        @DisplayName("when a deletion fails")
        class WhenADeletionFails {
            @Test
            void should_throw_so_that_the_message_is_redelivered_when_a_deletion_failed() {
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(1, 1));

                assertThatThrownBy(() -> service.execute("msg-1"))
                    .isInstanceOfSatisfying(
                        ConnectorAttachmentCleanupException.class, e -> {
                            assertThat(e.getMessageIdentifier()).isEqualTo("msg-1");
                            assertThat(e.getFailedCount()).isEqualTo(1);
                        }
                    );
            }

            @Test
            void should_not_mark_the_message_as_deleted() {
                when(filesDeleter.deleteActiveAttachmentsOf("msg-1"))
                    .thenReturn(new ConnectorAttachmentFilesDeleter.Result(1, 1));

                assertThatThrownBy(() -> service.execute("msg-1"))
                    .isInstanceOf(ConnectorAttachmentCleanupException.class);

                verifyNoInteractions(messageRepository);
            }
        }

        @Nested
        @DisplayName("when the message identifier is null")
        class WhenTheMessageIdentifierIsNull {
            @Test
            void should_throw_and_do_nothing() {
                assertThatThrownBy(() -> service.execute(null))
                    .isInstanceOf(NullPointerException.class);

                verifyNoInteractions(filesDeleter, messageRepository);
            }
        }
    }
}
