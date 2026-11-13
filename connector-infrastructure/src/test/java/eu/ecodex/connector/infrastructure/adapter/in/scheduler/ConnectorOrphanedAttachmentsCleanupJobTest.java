/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.scheduler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import eu.ecodex.connector.application.port.api.attachment.ConnectorCleanupOrphanedAttachments;
import eu.ecodex.connector.infrastructure.config.property.deletion.ConnectorDeletionProperties;
import eu.ecodex.connector.infrastructure.config.property.deletion.DeletionRule;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorOrphanedAttachmentsCleanupJob")
public class ConnectorOrphanedAttachmentsCleanupJobTest {
    private static final Instant NOW = Instant.parse("2026-10-28T22:00:00Z");

    @Mock
    private ConnectorCleanupOrphanedAttachments cleanupOrphanedAttachments;

    private ConnectorDeletionProperties properties;
    private ConnectorOrphanedAttachmentsCleanupJob job;

    @BeforeEach
    void setUp() {
        properties = new ConnectorDeletionProperties();
        job = new ConnectorOrphanedAttachmentsCleanupJob(
            cleanupOrphanedAttachments,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Nested
    @DisplayName("run")
    class Run {
        @Test
        void executes_the_use_case_with_now_minus_the_default_orphan_threshold() {
            job.run();

            verify(cleanupOrphanedAttachments).execute(NOW.minus(Duration.ofDays(1)));
        }

        @Test
        void uses_the_configured_orphan_threshold() {
            properties.getOrphan().setThreshold(Duration.ofHours(6));

            job.run();

            verify(cleanupOrphanedAttachments).execute(NOW.minus(Duration.ofHours(6)));
        }

        @Test
        void calls_the_use_case_exactly_once() {
            job.run();

            verify(cleanupOrphanedAttachments, times(1)).execute(any());
            verifyNoMoreInteractions(cleanupOrphanedAttachments);
        }

        @Test
        void lets_the_use_case_exception_propagate() {
            doThrow(new IllegalStateException("boom"))
                .when(cleanupOrphanedAttachments).execute(any());

            assertThatThrownBy(() -> job.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        }
    }

    @Nested
    @DisplayName("scheduling configuration")
    class SchedulingConfiguration {
        @Test
        void is_scheduled_with_the_orphan_cron_property_and_an_hourly_default() throws Exception {
            var scheduled = ConnectorOrphanedAttachmentsCleanupJob.class
                .getDeclaredMethod("run")
                .getAnnotation(Scheduled.class);

            assertThat(scheduled).isNotNull();
            assertThat(scheduled.cron())
                .isEqualTo("${connector.file-deletion.orphan.cron:0 0 0 * * *}");
        }
    }
}
