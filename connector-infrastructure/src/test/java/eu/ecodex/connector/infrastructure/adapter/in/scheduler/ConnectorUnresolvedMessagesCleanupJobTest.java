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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.port.api.message.ConnectorCleanupUnresolvedMessages;
import eu.ecodex.connector.infrastructure.config.property.deletion.ConnectorDeletionProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorUnresolvedMessagesCleanupJob")
class ConnectorUnresolvedMessagesCleanupJobTest {
    private static final Instant NOW = Instant.parse("2026-10-29T19:00:00Z");

    @Mock
    private ConnectorCleanupUnresolvedMessages cleanupUnresolvedMessagesAttachments;

    private ConnectorDeletionProperties properties;
    private ConnectorUnresolvedMessagesCleanupJob job;

    @BeforeEach
    void setUp() {
        properties = mock(ConnectorDeletionProperties.class, RETURNS_DEEP_STUBS);
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        job = new ConnectorUnresolvedMessagesCleanupJob(
            properties,
            clock,
            cleanupUnresolvedMessagesAttachments
        );
    }

    @Nested
    @DisplayName("run")
    class Run {
        @Test
        void should_clean_up_with_the_threshold_computed_from_the_clock_and_the_properties() {
            when(properties.getUnresolvedMessages().getThreshold())
                .thenReturn(Duration.ofDays(7));

            job.run();

            verify(cleanupUnresolvedMessagesAttachments)
                .execute(Instant.parse("2026-10-22T19:00:00Z"));
        }

        @ParameterizedTest
        @ValueSource(longs = {1, 30, 90})
        void should_use_the_configured_threshold_in_days(long days) {
            when(properties.getUnresolvedMessages().getThreshold())
                .thenReturn(Duration.ofDays(days));

            job.run();

            verify(cleanupUnresolvedMessagesAttachments).execute(NOW.minus(Duration.ofDays(days)));
        }

        @Test
        void should_use_the_current_instant_when_the_threshold_is_zero() {
            when(properties.getUnresolvedMessages().getThreshold()).thenReturn(Duration.ZERO);

            job.run();

            verify(cleanupUnresolvedMessagesAttachments).execute(NOW);
        }

        @Test
        void should_propagate_the_failure_of_the_cleanup_use_case() {
            when(properties.getUnresolvedMessages().getThreshold())
                .thenReturn(Duration.ofDays(7));
            doThrow(new IllegalStateException("cleanup failed"))
                .when(cleanupUnresolvedMessagesAttachments).execute(Instant.parse(
                    "2026-10-22T19:00:00Z"));

            assertThatThrownBy(() -> job.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cleanup failed");
        }
    }

    @Nested
    @DisplayName("scheduling configuration")
    class SchedulingConfiguration {
        private String cronExpression() throws NoSuchMethodException {
            return ConnectorUnresolvedMessagesCleanupJob.class
                .getMethod("run")
                .getAnnotation(Scheduled.class)
                .cron();
        }

        @Test
        void should_be_scheduled_with_the_unresolved_messages_cron_property()
            throws NoSuchMethodException {
            assertThat(cronExpression())
                .startsWith("${connector.file-deletion.unresolved-messages.cron:");
        }

        @Test
        void should_default_to_every_day_at_ten_past_midnight() throws NoSuchMethodException {
            assertThat(cronExpression()).endsWith(":0 10 0 * * *}");
        }
    }
}

