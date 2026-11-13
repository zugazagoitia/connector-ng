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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import eu.ecodex.connector.FixedClockConfiguration;
import eu.ecodex.connector.infrastructure.adapter.in.scheduler.ConnectorOrphanedAttachmentsCleanupJob;
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
    "connector.file-deletion.orphan.threshold=1d"
})
@Sql(
    statements = "DELETE FROM connector_business_domains WHERE id > 0",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
@Sql(
    scripts = {
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/message.sql",
        "classpath:sql/attachment.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@DisplayName("OrphanedAttachmentsCleanupIT CRON")
class OrphanedAttachmentsCleanupIT extends AbstractSchedulerTest {
    private static final String ORPHAN = "d98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file";
    private static final String LINKED = "6aeef356-d580-4b94-a569-250435ac3ec5_fake_file";
    // Orphan (no message) that is already soft deleted in attachment.sql
    private static final String ALREADY_DELETED = "f07d1bfd-685a-4fee-b70d-a582cd0711c7_test_attachment";

    @Autowired
    private ConnectorOrphanedAttachmentsCleanupJob job;

    @AfterEach
    void cleanUp() {
        cleanDb();
    }

    private Integer rowCount(String identifier) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM connector_message_attachments WHERE identifier = ?",
            Integer.class,
            identifier
        );
    }

    private Object deletedAt(String identifier) {
        return jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM connector_message_attachments WHERE identifier = ?",
            Object.class,
            identifier
        );
    }

    @Nested
    @DisplayName("when the storage deletion succeeds")
    class WhenTheStorageDeletionSucceeds {
        @Test
        void should_delete_the_orphan_file_and_remove_its_row() {
            job.run();

            verify(fileStorageProvider).deleteByIdentifier(ORPHAN);
            assertThat(rowCount(ORPHAN)).isZero();
        }

        @Test
        void should_leave_attachments_linked_to_a_message_untouched() {
            job.run();

            verify(fileStorageProvider, never()).deleteByIdentifier(LINKED);
            assertThat(rowCount(LINKED)).isEqualTo(1);
            assertThat(deletedAt(LINKED)).isNull();
        }

        @Test
        void should_leave_the_orphans_already_marked_as_deleted_untouched() {
            job.run();

            verify(fileStorageProvider, never()).deleteByIdentifier(ALREADY_DELETED);
            assertThat(rowCount(ALREADY_DELETED)).isEqualTo(1);
            assertThat(deletedAt(ALREADY_DELETED)).isNotNull();
        }

        @Test
        void should_not_mark_the_orphan_as_deleted_but_remove_it() {
            job.run();

            assertThat(rowCount(ORPHAN)).isZero();
        }

        @Test
        void should_be_idempotent_when_run_twice() {
            job.run();
            clearInvocations(fileStorageProvider);

            assertThatCode(job::run).doesNotThrowAnyException();

            // the row is gone, so it is not picked up again
            verifyNoInteractions(fileStorageProvider);
            assertThat(rowCount(ORPHAN)).isZero();
        }
    }

    @Nested
    @DisplayName("when the storage deletion fails")
    class WhenTheStorageDeletionFails {
        @BeforeEach
        void givenTheStorageIsUnavailable() {
            doThrow(new RuntimeException("S3 unavailable"))
                .when(fileStorageProvider).deleteByIdentifier(ORPHAN);
        }

        @Test
        void should_keep_the_row_so_the_orphan_is_retried_next_run() {
            assertThatCode(job::run).doesNotThrowAnyException();

            assertThat(rowCount(ORPHAN)).isEqualTo(1);
            assertThat(deletedAt(ORPHAN)).isNull();
        }

        @Test
        void should_retry_and_remove_the_row_on_the_next_run() {
            job.run();
            doNothing().when(fileStorageProvider).deleteByIdentifier(ORPHAN);

            job.run();

            assertThat(rowCount(ORPHAN)).isZero();
        }
    }
}
