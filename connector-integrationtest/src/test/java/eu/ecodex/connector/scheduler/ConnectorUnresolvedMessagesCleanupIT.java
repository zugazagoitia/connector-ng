/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.ecodex.connector.FixedClockConfiguration;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentException;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.infrastructure.adapter.in.scheduler.ConnectorUnresolvedMessagesCleanupJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@Import(FixedClockConfiguration.class)
@TestPropertySource(properties = {
    "connector.scheduling.enabled=false",
    "connector.file-deletion.unresolved-messages.threshold=1d"
})
@Sql(
    statements = "DELETE FROM connector_business_domains WHERE id IS NOT NULL",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = {
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-service.sql",
        "classpath:sql/processing-mode-action.sql",
        "classpath:sql/message.sql",
        "classpath:sql/message-as4-properties.sql",
        "classpath:sql/attachment.sql",
        "classpath:sql/message-business-content.sql",
        "classpath:sql/message-business-document.sql",
        "classpath:sql/evidence.sql",
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@DisplayName("ConnectorUnresolvedMessagesCleanupIT CRON")
public class ConnectorUnresolvedMessagesCleanupIT extends AbstractSchedulerTest {
    private static final String MESSAGE_1 =
        "fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu";
    private static final String MESSAGE_2 =
        "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu";
    private static final String MESSAGE_3 =
        "7a169fa8-1f0d-4a2c-aade-796b0b02fe58@connector.ecodex.eu";
    private static final String MESSAGE_4 =
        "3fae4358-7cc9-4929-a17b-4432cbb8b9cc@connector.ecodex.eu";
    private static final String[] ALL_MESSAGES = {MESSAGE_1, MESSAGE_2, MESSAGE_3, MESSAGE_4};

    private static final String ORPHAN_ATTACHMENT =
        "d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file";
    private static final String ATTACHMENT_OF_MESSAGE_3 =
        "b9bd63c7-5a5a-4701-a23c-e5e714436585_messageContent";

    @Autowired
    private ConnectorUnresolvedMessagesCleanupJob job;
    @Autowired
    private ConnectorMessageRepository messageRepository;

    private boolean messageExists(String messageIdentifier) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM connector_messages WHERE identifier = ?",
            Integer.class,
            messageIdentifier
        ) > 0;
    }

    /** Counts the rows of a child table (it must have a message_id column) of a message. */
    private Integer childRowsOf(String table, String messageIdentifier) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + table + " c JOIN connector_messages m "
                + "ON c.message_id = m.id WHERE m.identifier = ?",
            Integer.class,
            messageIdentifier
        );
    }

    private int attachmentsOf(String messageIdentifier) {
        return childRowsOf("connector_message_attachments", messageIdentifier);
    }

    private Integer deletedAttachmentsOf(String messageIdentifier) {
        return jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*) FROM connector_message_attachments a
                  JOIN connector_messages m ON a.message_id = m.id
                 WHERE m.identifier = ? AND a.deleted_at IS NOT NULL
                """,
            Integer.class,
            messageIdentifier
        );
    }

    private boolean attachmentExists(String attachmentIdentifier) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM connector_message_attachments WHERE identifier = ?",
            Integer.class,
            attachmentIdentifier
        ) > 0;
    }

    private boolean isAttachmentDeleted(String attachmentIdentifier) {
        return jdbcTemplate.queryForObject(
            "SELECT deleted_at IS NOT NULL FROM connector_message_attachments "
                + "WHERE identifier = ?",
            Boolean.class,
            attachmentIdentifier
        );
    }

    @AfterEach
    void cleanUp() {
        cleanDb();
    }

    @Nested
    @DisplayName("when all the unresolved messages can be cleaned up")
    class WhenAllTheUnresolvedMessagesCanBeCleanedUp {
        @Test
        void should_delete_every_unresolved_message() {
            job.run();

            for (var message : ALL_MESSAGES) {
                assertThat(messageExists(message)).isFalse();
            }
        }

        @Test
        void should_delete_the_attachments_of_every_unresolved_message() {
            job.run();

            for (var message : ALL_MESSAGES) {
                assertThat(attachmentsOf(message)).isZero();
            }
        }

        @Test
        void should_delete_the_evidences_of_every_unresolved_message() {
            // adjust the table name to your schema
            job.run();

            for (var message : ALL_MESSAGES) {
                assertThat(childRowsOf("connector_message_evidences", message)).isZero();
            }
        }

        @Test
        void should_delete_the_content_of_the_attachments_from_the_storage() {
            job.run();

            verify(fileStorageProvider).deleteByIdentifier(ATTACHMENT_OF_MESSAGE_3);
        }

        @Test
        void should_not_delete_the_same_content_again_when_run_twice() {
            job.run();
            job.run();

            verify(fileStorageProvider, times(1)).deleteByIdentifier(ATTACHMENT_OF_MESSAGE_3);
        }
    }

    @Nested
    @DisplayName("when a message is already resolved")
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    class WhenAMessageIsAlreadyResolved {
        @Test
        void should_keep_a_confirmed_message_and_its_attachments() {
            var attachmentsBefore = attachmentsOf(MESSAGE_1);
            messageRepository.setAsConfirmed(MESSAGE_1);

            job.run();

            assertThat(messageExists(MESSAGE_1)).isTrue();
            assertThat(attachmentsOf(MESSAGE_1)).isEqualTo(attachmentsBefore);
            assertThat(deletedAttachmentsOf(MESSAGE_1)).isZero();
            assertThat(messageExists(MESSAGE_2)).isFalse();
        }

        @Test
        void should_keep_a_rejected_message_and_its_attachments() {
            var attachmentsBefore = attachmentsOf(MESSAGE_2);
            messageRepository.setAsRejected(MESSAGE_2);

            job.run();

            assertThat(messageExists(MESSAGE_2)).isTrue();
            assertThat(attachmentsOf(MESSAGE_2)).isEqualTo(attachmentsBefore);
            assertThat(deletedAttachmentsOf(MESSAGE_2)).isZero();
            assertThat(messageExists(MESSAGE_1)).isFalse();
        }
    }

    @Nested
    @DisplayName("orphan attachments")
    class OrphanAttachments {
        @Test
        void should_not_touch_the_orphan_attachments() {
            job.run();

            assertThat(attachmentExists(ORPHAN_ATTACHMENT)).isTrue();
            assertThat(isAttachmentDeleted(ORPHAN_ATTACHMENT)).isFalse();
            verify(fileStorageProvider, never()).deleteByIdentifier(ORPHAN_ATTACHMENT);
        }
    }

    @Nested
    @DisplayName("when the content of an attachment cannot be deleted")
    class WhenTheContentOfAnAttachmentCannotBeDeleted {
        @BeforeEach
        void given_the_storage_fails_for_an_attachment_of_message_3() {
            doThrow(new ConnectorMessageAttachmentException("storage error"))
                .when(fileStorageProvider).deleteByIdentifier(ATTACHMENT_OF_MESSAGE_3);
        }

        @Test
        void should_keep_the_message_so_that_the_files_are_retried() {
            job.run();

            assertThat(messageExists(MESSAGE_3)).isTrue();
        }

        @Test
        void should_keep_the_attachment_row_that_could_not_be_deleted_active() {
            job.run();

            assertThat(attachmentExists(ATTACHMENT_OF_MESSAGE_3)).isTrue();
            assertThat(isAttachmentDeleted(ATTACHMENT_OF_MESSAGE_3)).isFalse();
        }

        @Test
        void should_mark_the_other_attachments_of_the_message_as_deleted() {
            var attachmentsBefore = attachmentsOf(MESSAGE_3);

            job.run();

            assertThat(attachmentsOf(MESSAGE_3)).isEqualTo(attachmentsBefore);
            assertThat(deletedAttachmentsOf(MESSAGE_3)).isEqualTo(attachmentsBefore - 1);
        }

        @Test
        void should_still_delete_the_other_messages() {
            job.run();

            assertThat(messageExists(MESSAGE_4)).isFalse();
            assertThat(attachmentsOf(MESSAGE_4)).isZero();
        }

        @Test
        void should_delete_the_message_at_the_next_run_once_the_storage_works_again() {
            job.run();
            reset(fileStorageProvider);

            job.run();

            assertThat(messageExists(MESSAGE_3)).isFalse();
            assertThat(attachmentsOf(MESSAGE_3)).isZero();
        }
    }
}
