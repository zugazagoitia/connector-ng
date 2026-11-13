/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.message;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.filter.ConnectorAttachmentsListFilter;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.paging.ConnectorPageRequest;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@SuppressWarnings("DataFlowIssue")
@Sql(
    statements = "DELETE FROM connector_business_domains WHERE id IS NOT NULL",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@DisplayName("ConnectorDBMessageAttachmentRepository")
public class ConnectorDBMessageAttachmentRepositoryTest extends AbstractRepositoryTest {
    private static final String MESSAGE_ID =
        "fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu";
    @Autowired
    private ConnectorMessageAttachmentRepository repository;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/message.sql",
        "classpath:sql/attachment.sql",
    })
    private @interface WithMessageAndAttachmentData {
    }

    @Nested
    @DisplayName("save an attachment")
    class Save {
        @Test
        void should_save_the_attachment() {
            var attachment = MessageAttachmentTestFixtures.createAttachment();

            var savedAttachment = repository.save(attachment);

            assertThat(savedAttachment).isNotNull();
            assertThat(savedAttachment.identifier()).contains("_test_attachment");
            assertThat(savedAttachment.name()).isEqualTo("test_attachment.txt");
            assertThat(savedAttachment.size()).isEqualTo(100L);
            assertThat(savedAttachment.contentType()).isEqualTo("text/plain");
            assertThat(savedAttachment.description()).isNotBlank();
            assertThat(savedAttachment.storage()).isEqualTo(ConnectorAttachmentStorage.S3_BUCKET);
        }
    }

    @Nested
    @DisplayName("find by identifier")
    class FindByIdentifier {
        @Test
        void should_find_the_attachment() {
            var attachment = MessageAttachmentTestFixtures.createAttachment();
            repository.save(attachment);

            var foundAttachment = repository.findByIdentifier(attachment.identifier());

            assertThat(foundAttachment).isNotNull();
            assertThat(foundAttachment.identifier()).isEqualTo(attachment.identifier());
        }

        @Test
        void should_return_null_when_the_identifier_is_unknown() {
            var attachment = MessageAttachmentTestFixtures.createAttachment();

            var foundAttachment = repository.findByIdentifier(attachment.identifier());

            assertThat(foundAttachment).isNull();
        }

        @Test
        void should_throw_when_the_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("find all active by message identifier")
    class FindAllActiveByMessageIdentifier {
        private static final String ORPHAN_IDENTIFIER =
            "d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file";
        private static final String ASICS_IDENTIFIER_OF_OTHER_MESSAGE =
            "d9368fda-92f2-498f-95bf-1ca6f4985b85_ASIC-S";
        private static final List<String> MESSAGE_ATTACHMENT_IDENTIFIERS = List.of(
            "6aeef356-d580-4b94-a569-250435ac3ec5_fake_file",
            "c12f879b-3c9a-4d26-b36c-b6d67a84f0ed_test_attachment",
            "1875f1b7-5ea2-494d-8932-29966189f02c_messageContent",
            "dd5a7178-c1aa-4723-bc7b-d3f007ccd7ac_messageContent"
        );

        @Test
        @WithMessageAndAttachmentData
        void should_return_all_the_active_attachments_of_the_message() {
            var attachments = repository.findAllActiveByMessageIdentifier(MESSAGE_ID);

            assertThat(attachments)
                .extracting(ConnectorMessageAttachment::identifier)
                .containsExactlyInAnyOrderElementsOf(MESSAGE_ATTACHMENT_IDENTIFIERS);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_not_return_the_attachments_marked_as_deleted() {
            var deletedIdentifier = MESSAGE_ATTACHMENT_IDENTIFIERS.getFirst();
            repository.markAsDeleted(List.of(deletedIdentifier), Instant.now());

            var attachments = repository.findAllActiveByMessageIdentifier(MESSAGE_ID);

            assertThat(attachments)
                .extracting(ConnectorMessageAttachment::identifier)
                .doesNotContain(deletedIdentifier)
                .containsExactlyInAnyOrderElementsOf(
                    MESSAGE_ATTACHMENT_IDENTIFIERS.subList(1, MESSAGE_ATTACHMENT_IDENTIFIERS.size())
                );
        }

        @Test
        @WithMessageAndAttachmentData
        void should_return_an_empty_list_when_all_the_attachments_are_deleted() {
            repository.markAsDeleted(MESSAGE_ATTACHMENT_IDENTIFIERS, Instant.now());

            var attachments = repository.findAllActiveByMessageIdentifier(MESSAGE_ID);

            assertThat(attachments).isEmpty();
        }

        @Test
        @WithMessageAndAttachmentData
        void should_not_return_the_attachments_of_other_messages() {
            var attachments = repository.findAllActiveByMessageIdentifier(MESSAGE_ID);

            assertThat(attachments)
                .extracting(ConnectorMessageAttachment::identifier)
                .doesNotContain(ASICS_IDENTIFIER_OF_OTHER_MESSAGE);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_not_return_the_orphan_attachments() {
            var attachments = repository.findAllActiveByMessageIdentifier(MESSAGE_ID);

            assertThat(attachments)
                .extracting(ConnectorMessageAttachment::identifier)
                .doesNotContain(ORPHAN_IDENTIFIER);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_return_an_empty_list_when_the_message_is_unknown() {
            var attachments =
                repository.findAllActiveByMessageIdentifier("unknown@connector.ecodex.eu");

            assertThat(attachments).isEmpty();
        }

        @Test
        void should_throw_when_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findAllActiveByMessageIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("find all")
    class FindAll {
        @Test
        void should_return_all_the_attachments() {
            var attachment = MessageAttachmentTestFixtures.createAttachment();
            repository.save(attachment);

            var pageRequest = ConnectorPageRequest.builder().page(0).size(20).build();
            var attachments = repository.findAll(ConnectorAttachmentsListFilter.of(pageRequest));

            assertThat(attachments).isNotNull();
            assertThat(attachments.content()).hasSize(1);
            assertThat(attachments.size()).isEqualTo(1);
            assertThat(attachments.totalElements()).isEqualTo(1L);
            assertThat(attachments.totalPages()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("attach to message")
    class AttachToMessage {
        @Test
        @WithMessageAndAttachmentData
        void should_attach_the_message_to_the_attachment() {
            repository.attachToMessage(
                "d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file",
                MESSAGE_ID
            );
        }

        @Test
        @WithMessageAndAttachmentData
        void should_fail_when_the_attachment_is_already_attached() {
            assertThrows(
                RuntimeException.class,
                () -> repository.attachToMessage(
                    "6aeef356-d580-4b94-a569-250435ac3ec5_fake_file",
                    MESSAGE_ID
                )
            );
        }

        @Test
        void should_throw_when_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.attachToMessage(
                    "6aeef356-d580-4b94-a569-250435ac3ec5_fake_file",
                    null
                )
            );
        }

        @Test
        void should_throw_when_the_attachment_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.attachToMessage(
                    null,
                    "e4d9a3a5-42e8-4eeb-9236-678ecfbc0eb4"
                )
            );
        }

        @Test
        void should_throw_when_both_identifiers_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.attachToMessage(null, null)
            );
        }
    }

    @Nested
    @DisplayName("find all orphaned")
    class FindAllOrphaned {
        private static final String ORPHAN_IDENTIFIER =
            "d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file";

        @Test
        @WithMessageAndAttachmentData
        void should_return_the_orphan_when_it_is_older_than_the_threshold() {
            var threshold = Instant.now().plus(Duration.ofDays(1));

            var orphans = repository.findAllOrphaned(threshold);

            assertThat(orphans)
                .extracting(ConnectorMessageAttachment::identifier)
                .containsExactly(ORPHAN_IDENTIFIER);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_not_return_the_orphan_when_it_is_more_recent_than_the_threshold() {
            var threshold = Instant.now().minus(Duration.ofDays(1));

            var orphans = repository.findAllOrphaned(threshold);

            assertThat(orphans).isEmpty();
        }

        @Test
        @WithMessageAndAttachmentData
        void should_not_return_attachments_linked_to_a_message() {
            var threshold = Instant.now().plus(Duration.ofDays(1));

            var orphans = repository.findAllOrphaned(threshold);

            assertThat(orphans)
                .extracting(ConnectorMessageAttachment::identifier)
                .doesNotContain("6aeef356-d580-4b94-a569-250435ac3ec5_fake_file");
        }

        @Test
        @WithMessageAndAttachmentData
        void should_not_return_the_orphan_once_it_is_marked_as_deleted() {
            var threshold = Instant.now().plus(Duration.ofDays(1));
            repository.markAsDeleted(List.of(ORPHAN_IDENTIFIER), Instant.now());

            var orphans = repository.findAllOrphaned(threshold);

            assertThat(orphans).isEmpty();
        }

        @Test
        void should_return_an_empty_list_when_there_is_no_attachment() {
            var orphans = repository.findAllOrphaned(Instant.now().plus(Duration.ofDays(1)));

            assertThat(orphans).isEmpty();
        }
    }

    @Nested
    @DisplayName("mark as deleted")
    class MarkAsDeleted {
        private static final String IDENTIFIER_1 =
            "d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file";
        private static final String IDENTIFIER_2 =
            "6aeef356-d580-4b94-a569-250435ac3ec5_fake_file";
        private static final String IDENTIFIER_3 =
            "c12f879b-3c9a-4d26-b36c-b6d67a84f0ed_test_attachment";

        private int countDeleted(String identifier) {
            return jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*) FROM connector_message_attachments
                     WHERE identifier = ? AND deleted_at IS NOT NULL
                    """,
                Integer.class,
                identifier
            );
        }

        @Test
        @WithMessageAndAttachmentData
        void should_set_the_deletion_date_on_the_given_attachments_only() {
            repository.markAsDeleted(List.of(IDENTIFIER_1, IDENTIFIER_2), Instant.now());

            assertThat(countDeleted(IDENTIFIER_1)).isEqualTo(1);
            assertThat(countDeleted(IDENTIFIER_2)).isEqualTo(1);
            assertThat(countDeleted(IDENTIFIER_3)).isZero();
        }

        @Test
        @WithMessageAndAttachmentData
        void should_ignore_unknown_identifiers() {
            repository.markAsDeleted(List.of(IDENTIFIER_1, "unknown_identifier"), Instant.now());

            assertThat(countDeleted(IDENTIFIER_1)).isEqualTo(1);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_do_nothing_when_the_list_is_empty() {
            repository.markAsDeleted(List.of(), Instant.now());

            var deletedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_message_attachments WHERE deleted_at IS NOT NULL",
                Integer.class
            );
            assertThat(deletedCount).isEqualTo(1);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_be_idempotent_when_called_twice() {
            repository.markAsDeleted(List.of(IDENTIFIER_1), Instant.now());

            assertThatCode(() -> repository.markAsDeleted(List.of(IDENTIFIER_1), Instant.now()))
                .doesNotThrowAnyException();
            assertThat(countDeleted(IDENTIFIER_1)).isEqualTo(1);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_process_more_identifiers_than_the_batch_size() {
            // Pads with unknown identifiers so the list spans several batches
            var identifiers = new ArrayList<String>();
            identifiers.add(IDENTIFIER_1);
            for (int i = 0; i < 2_500; i++) {
                identifiers.add("unknown_identifier_" + i);
            }
            identifiers.add(IDENTIFIER_3);

            repository.markAsDeleted(identifiers, Instant.now());

            assertThat(countDeleted(IDENTIFIER_1)).isEqualTo(1);
            assertThat(countDeleted(IDENTIFIER_3)).isEqualTo(1);
        }

        @Test
        void should_throw_when_the_identifiers_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.markAsDeleted(null, Instant.now())
            );
        }
    }

    @Nested
    @DisplayName("delete all by identifiers")
    class DeleteAllByIdentifiers {
        private static final String IDENTIFIER_1 =
            "d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file";
        private static final String IDENTIFIER_2 =
            "6aeef356-d580-4b94-a569-250435ac3ec5_fake_file";
        private static final String IDENTIFIER_3 =
            "c12f879b-3c9a-4d26-b36c-b6d67a84f0ed_test_attachment";
        private static final String ALREADY_DELETED_IDENTIFIER =
            "f07d1bfd-685a-4fee-b70d-a582cd0711c7_test_attachment";

        private int countRows(String identifier) {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_message_attachments WHERE identifier = ?",
                Integer.class,
                identifier
            );
        }

        private int countAllRows() {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_message_attachments",
                Integer.class
            );
        }

        @Test
        @WithMessageAndAttachmentData
        void should_remove_the_rows_of_the_given_attachments_only() {
            repository.deleteAllByIdentifiers(List.of(IDENTIFIER_1, IDENTIFIER_2));

            assertThat(countRows(IDENTIFIER_1)).isZero();
            assertThat(countRows(IDENTIFIER_2)).isZero();
            assertThat(countRows(IDENTIFIER_3)).isEqualTo(1);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_remove_the_rows_instead_of_marking_them_as_deleted() {
            var before = countAllRows();

            repository.deleteAllByIdentifiers(List.of(IDENTIFIER_1));

            assertThat(countAllRows()).isEqualTo(before - 1);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_remove_an_attachment_that_is_already_marked_as_deleted() {
            assertThat(countRows(ALREADY_DELETED_IDENTIFIER)).isEqualTo(1);

            repository.deleteAllByIdentifiers(List.of(ALREADY_DELETED_IDENTIFIER));

            assertThat(countRows(ALREADY_DELETED_IDENTIFIER)).isZero();
        }

        @Test
        @WithMessageAndAttachmentData
        void should_ignore_unknown_identifiers() {
            var before = countAllRows();

            repository.deleteAllByIdentifiers(List.of(IDENTIFIER_1, "unknown_identifier"));

            assertThat(countRows(IDENTIFIER_1)).isZero();
            assertThat(countAllRows()).isEqualTo(before - 1);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_do_nothing_when_the_list_is_empty() {
            var before = countAllRows();

            repository.deleteAllByIdentifiers(List.of());

            assertThat(countAllRows()).isEqualTo(before);
        }

        @Test
        @WithMessageAndAttachmentData
        void should_be_idempotent_when_called_twice() {
            repository.deleteAllByIdentifiers(List.of(IDENTIFIER_1));

            assertThatCode(() -> repository.deleteAllByIdentifiers(List.of(IDENTIFIER_1)))
                .doesNotThrowAnyException();
            assertThat(countRows(IDENTIFIER_1)).isZero();
        }

        @Test
        @WithMessageAndAttachmentData
        void should_process_more_identifiers_than_the_batch_size() {
            var identifiers = new ArrayList<String>();

            identifiers.add(IDENTIFIER_1);

            for (int i = 0; i < 2_500; i++) {
                identifiers.add("unknown_identifier_" + i);
            }

            identifiers.add(IDENTIFIER_3);

            repository.deleteAllByIdentifiers(identifiers);

            assertThat(countRows(IDENTIFIER_1)).isZero();
            assertThat(countRows(IDENTIFIER_3)).isZero();
            assertThat(countRows(IDENTIFIER_2)).isEqualTo(1);
        }

        @Test
        void should_throw_when_the_identifiers_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.deleteAllByIdentifiers(null)
            );
        }
    }
}
