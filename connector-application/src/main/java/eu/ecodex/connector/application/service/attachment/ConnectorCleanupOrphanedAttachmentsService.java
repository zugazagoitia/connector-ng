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

import eu.ecodex.connector.application.port.api.attachment.ConnectorCleanupOrphanedAttachments;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import java.time.Instant;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ConnectorCleanupOrphanedAttachments}.
 *
 * <p>The service finds the attachments that were uploaded but never linked to a message and
 * created before the given threshold, then deletes them. It is called by
 * {@code ConnectorOrphanedAttachmentsCleanupJob}.
 */
@Slf4j
@Service
public class ConnectorCleanupOrphanedAttachmentsService implements
    ConnectorCleanupOrphanedAttachments {
    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorAttachmentFilesDeleter filesDeleter;

    /**
     * Creates the service.
     *
     * @param attachmentRepository the repository used to find the orphaned attachments (must not be
     *                             null)
     * @param filesDeleter         the component that deletes the content of the attachments from
     *                             the file storage and marks them as deleted (must not be null)
     */
    public ConnectorCleanupOrphanedAttachmentsService(
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorAttachmentFilesDeleter filesDeleter) {
        this.attachmentRepository = attachmentRepository;
        this.filesDeleter = filesDeleter;
    }

    @Override
    public void execute(@NonNull Instant threshold) {
        var orphaned = attachmentRepository.findAllOrphaned(threshold);

        if (orphaned.isEmpty()) {
            log.debug("No orphaned attachments older than {}", threshold);
            return;
        }

        log.info("{} orphaned attachments older than {} to delete", orphaned.size(), threshold);

        var result = filesDeleter.purge(orphaned);

        log.info(
            "Orphaned attachments cleanup done: [{}] deleted, [{}] failed",
            result.deleted(), result.failed()
        );
    }
}
