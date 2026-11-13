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

import eu.ecodex.connector.application.port.api.attachment.ConnectorCleanupOrphanedAttachments;
import eu.ecodex.connector.application.port.api.message.ConnectorMessageAttachmentLinker;
import eu.ecodex.connector.infrastructure.config.property.deletion.ConnectorDeletionProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that cleans up orphaned attachments.
 *
 * <p>An attachment is orphaned when it was uploaded/created but
 * never linked to a message (see {@link ConnectorMessageAttachmentLinker}). On each run, the job
 * computes a threshold instant (now minus the configured orphan threshold) and delegates to
 * {@link ConnectorCleanupOrphanedAttachments}, which removes the orphaned attachments older than
 * that threshold.
 *
 * <p>The schedule is configured with the property {@code connector.file-deletion.orphan.cron},
 * which defaults to {@code 0 0 0 * * *} (every day at 00:00:00).
 */
@Component
public class ConnectorOrphanedAttachmentsCleanupJob {
    private final ConnectorCleanupOrphanedAttachments cleanupOrphanedAttachments;
    private final ConnectorDeletionProperties properties;
    private final Clock clock;

    /**
     * Creates the job.
     *
     * @param cleanupOrphanedAttachments the use case that deletes the orphaned attachments (must
     *                                   not be null)
     * @param properties                 the deletion properties, providing the orphan threshold
     *                                   (must not be null)
     * @param clock                      the clock used to compute the threshold, which allows a
     *                                   fixed clock in tests (must not be null)
     */
    public ConnectorOrphanedAttachmentsCleanupJob(
        ConnectorCleanupOrphanedAttachments cleanupOrphanedAttachments,
        ConnectorDeletionProperties properties,
        Clock clock) {
        this.cleanupOrphanedAttachments = cleanupOrphanedAttachments;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Runs the cleanup: deletes the orphaned attachments created before the threshold, that is, the
     * current instant minus the configured orphan threshold.
     *
     * <p>Invoked by the scheduler according to {@code connector.file-deletion.orphan.cron}; it is
     * not meant to be called directly. A failure thrown by the use case propagates to the
     * scheduler, which logs it and runs the job again at the next trigger.
     */
    @Scheduled(cron = "${connector.file-deletion.orphan.cron:0 0 0 * * *}")
    public void run() {
        var threshold = Instant.now(clock).minus(properties.getOrphan().getThreshold());
        cleanupOrphanedAttachments.execute(threshold);
    }
}
