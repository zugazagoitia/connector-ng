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

import eu.ecodex.connector.application.port.api.message.ConnectorCleanupUnresolvedMessages;
import eu.ecodex.connector.infrastructure.config.property.deletion.ConnectorDeletionProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that cleans up the attachments of unresolved messages.
 *
 * <p>On each run, the job computes a threshold instant (now minus the configured unresolved
 * messages threshold) and delegates to {@link ConnectorCleanupUnresolvedMessages}, which
 * removes the attachments of the messages that are still unresolved at that point.
 *
 * <p>The schedule is configured with the property
 * {@code connector.file-deletion.unresolved-messages.cron}. The threshold comes from the unresolved
 * messages settings of {@link ConnectorDeletionProperties}.
 */
@Component
public class ConnectorUnresolvedMessagesCleanupJob {
    private final ConnectorDeletionProperties properties;
    private final Clock clock;
    private final ConnectorCleanupUnresolvedMessages
        cleanupUnresolvedMessagesAttachments;

    /**
     * Creates the job.
     *
     * @param properties                           the deletion properties, providing the unresolved
     *                                             messages threshold (must not be null)
     * @param clock                                the clock used to compute the threshold, which
     *                                             allows a fixed clock in tests (must not be null)
     * @param cleanupUnresolvedMessagesAttachments the use case that deletes the attachments of
     *                                             unresolved messages (must not be null)
     */
    public ConnectorUnresolvedMessagesCleanupJob(
        ConnectorDeletionProperties properties,
        Clock clock,
        ConnectorCleanupUnresolvedMessages cleanupUnresolvedMessagesAttachments) {
        this.properties = properties;
        this.clock = clock;
        this.cleanupUnresolvedMessagesAttachments = cleanupUnresolvedMessagesAttachments;
    }

    /**
     * Runs the cleanup: deletes the attachments of the unresolved messages created before the
     * threshold, that is, the current instant minus the configured unresolved messages threshold.
     *
     * <p>Invoked by the scheduler according to
     * {@code connector.file-deletion.unresolved-messages.cron}; it is not meant to be called
     * directly. A failure thrown by the use case propagates to the scheduler, which logs it and
     * runs the job again at the next trigger.
     */
    @Scheduled(cron = "${connector.file-deletion.unresolved-messages.cron:0 10 0 * * *}")
    public void run() {
        var threshold = Instant.now(clock).minus(properties.getUnresolvedMessages().getThreshold());
        cleanupUnresolvedMessagesAttachments.execute(threshold);
    }
}
