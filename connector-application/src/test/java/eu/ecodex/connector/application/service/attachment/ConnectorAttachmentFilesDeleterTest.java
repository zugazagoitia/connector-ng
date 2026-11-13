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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorAttachmentFilesDeleter")
class ConnectorAttachmentFilesDeleterTest {
    private static final Instant NOW = Instant.parse("2026-10-28T22:00:00Z");

    @Mock
    private ConnectorMessageAttachmentRepository attachmentRepository;
    @Mock
    private ConnectorFileStorageProvider fileStorageProvider;

    private ConnectorAttachmentFilesDeleter deleter;

    private ConnectorMessageAttachment attachment(String identifier) {
        return ConnectorMessageAttachment.builder()
                                         .identifier(identifier)
                                         .build();
    }

    @BeforeEach
    void set_up() {
        deleter = new ConnectorAttachmentFilesDeleter(
            attachmentRepository,
            fileStorageProvider,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Nested
    @DisplayName("delete")
    class Delete {
        @Test
        void should_do_nothing_when_there_are_no_attachments() {
            var result = deleter.delete(List.of());

            assertThat(result).isEqualTo(ConnectorAttachmentFilesDeleter.Result.NONE);
            verifyNoInteractions(fileStorageProvider, attachmentRepository);
        }

        @Test
        void should_delete_every_file_and_mark_them_as_deleted_with_the_clock_instant() {
            var result = deleter.delete(List.of(attachment("id-1"), attachment("id-2")));

            verify(fileStorageProvider).deleteByIdentifier("id-1");
            verify(fileStorageProvider).deleteByIdentifier("id-2");
            verify(attachmentRepository).markAsDeleted(List.of("id-1", "id-2"), NOW);
            assertThat(result.deleted()).isEqualTo(2);
            assertThat(result.failed()).isZero();
        }

        @Test
        void should_keep_the_rows_when_marking_them_as_deleted() {
            deleter.delete(List.of(attachment("id-1")));

            verify(attachmentRepository, never()).deleteAllByIdentifiers(anyList());
        }

        @Test
        void should_continue_and_mark_only_the_successful_ones_when_one_deletion_fails() {
            lenient().doThrow(new RuntimeException("S3 unavailable"))
                     .when(fileStorageProvider).deleteByIdentifier("id-2");

            var result = deleter.delete(
                List.of(attachment("id-1"), attachment("id-2"), attachment("id-3")));

            verify(fileStorageProvider).deleteByIdentifier("id-3");
            verify(attachmentRepository).markAsDeleted(List.of("id-1", "id-3"), NOW);
            assertThat(result.deleted()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(1);
        }

        @Test
        void should_not_mark_anything_as_deleted_when_all_deletions_fail() {
            doThrow(new RuntimeException("S3 unavailable"))
                .when(fileStorageProvider).deleteByIdentifier(anyString());

            var result = deleter.delete(List.of(attachment("id-1"), attachment("id-2")));

            verify(attachmentRepository, never()).markAsDeleted(anyList(), any());
            assertThat(result.deleted()).isZero();
            assertThat(result.failed()).isEqualTo(2);
        }

        @Test
        void should_propagate_the_failure_when_marking_the_attachments_as_deleted_fails() {
            doThrow(new IllegalStateException("database error"))
                .when(attachmentRepository).markAsDeleted(anyList(), any());

            var attachments = List.of(attachment("id-1"));

            assertThatThrownBy(() -> deleter.delete(attachments))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database error");
        }

        @Test
        void should_throw_when_the_attachments_are_null() {
            assertThatThrownBy(() -> deleter.delete(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @SuppressWarnings("DataFlowIssue")
    @DisplayName("purge")
    class Purge {
        @Test
        void should_do_nothing_when_there_are_no_attachments() {
            var result = deleter.purge(List.of());

            assertThat(result).isEqualTo(ConnectorAttachmentFilesDeleter.Result.NONE);
            verifyNoInteractions(fileStorageProvider, attachmentRepository);
        }

        @Test
        void should_delete_every_file_and_remove_their_rows() {
            var result = deleter.purge(List.of(attachment("id-1"), attachment("id-2")));

            verify(fileStorageProvider).deleteByIdentifier("id-1");
            verify(fileStorageProvider).deleteByIdentifier("id-2");
            verify(attachmentRepository).deleteAllByIdentifiers(List.of("id-1", "id-2"));
            assertThat(result.deleted()).isEqualTo(2);
            assertThat(result.failed()).isZero();
        }

        @Test
        void should_not_use_the_soft_delete() {
            deleter.purge(List.of(attachment("id-1")));

            verify(attachmentRepository, never()).markAsDeleted(anyList(), any());
        }

        @Test
        void should_continue_and_remove_only_the_rows_of_the_deleted_files_when_one_fails() {
            lenient().doThrow(new RuntimeException("S3 unavailable"))
                     .when(fileStorageProvider).deleteByIdentifier("id-2");

            var result = deleter.purge(
                List.of(attachment("id-1"), attachment("id-2"), attachment("id-3")));

            verify(fileStorageProvider).deleteByIdentifier("id-3");
            verify(attachmentRepository).deleteAllByIdentifiers(List.of("id-1", "id-3"));
            assertThat(result.deleted()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(1);
        }

        @Test
        void should_not_remove_any_row_when_all_deletions_fail() {
            doThrow(new RuntimeException("S3 unavailable"))
                .when(fileStorageProvider).deleteByIdentifier(anyString());

            var result = deleter.purge(List.of(attachment("id-1"), attachment("id-2")));

            verify(attachmentRepository, never()).deleteAllByIdentifiers(anyList());
            assertThat(result.deleted()).isZero();
            assertThat(result.failed()).isEqualTo(2);
        }

        @Test
        void should_propagate_the_failure_when_removing_the_rows_fails() {
            doThrow(new IllegalStateException("database error"))
                .when(attachmentRepository).deleteAllByIdentifiers(anyList());
            var attachments = List.of(attachment("id-1"));

            assertThatThrownBy(() -> deleter.purge(attachments))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database error");

            verify(fileStorageProvider).deleteByIdentifier("id-1");
        }

        @Test
        void should_throw_when_the_attachments_are_null() {
            assertThatThrownBy(() -> deleter.purge(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Nested
    @DisplayName("delete active attachments of a message")
    class DeleteActiveAttachmentsOf {
        @Test
        void should_delete_the_active_attachments_of_the_message() {
            when(attachmentRepository.findAllActiveByMessageIdentifier("msg-1"))
                .thenReturn(List.of(attachment("id-1")));

            var result = deleter.deleteActiveAttachmentsOf("msg-1");

            verify(fileStorageProvider).deleteByIdentifier("id-1");
            verify(attachmentRepository).markAsDeleted(List.of("id-1"), NOW);
            assertThat(result.deleted()).isEqualTo(1);
        }

        @Test
        void should_return_none_when_the_message_has_no_active_attachment() {
            when(attachmentRepository.findAllActiveByMessageIdentifier("msg-1"))
                .thenReturn(List.of());

            var result = deleter.deleteActiveAttachmentsOf("msg-1");

            assertThat(result).isEqualTo(ConnectorAttachmentFilesDeleter.Result.NONE);
            verifyNoInteractions(fileStorageProvider);
        }

        @Test
        void should_throw_when_the_message_identifier_is_null() {
            assertThatThrownBy(() -> deleter.deleteActiveAttachmentsOf(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("result")
    class ResultTest {
        @Test
        void should_add_the_deleted_and_failed_counts() {
            var first = new ConnectorAttachmentFilesDeleter.Result(2, 1);
            var second = new ConnectorAttachmentFilesDeleter.Result(3, 4);

            var sum = first.plus(second);

            assertThat(sum).isEqualTo(new ConnectorAttachmentFilesDeleter.Result(5, 5));
        }

        @Test
        void should_keep_the_result_unchanged_when_adding_none() {
            var result = new ConnectorAttachmentFilesDeleter.Result(2, 1);

            assertThat(result.plus(ConnectorAttachmentFilesDeleter.Result.NONE))
                .isEqualTo(result);
        }
    }
}
