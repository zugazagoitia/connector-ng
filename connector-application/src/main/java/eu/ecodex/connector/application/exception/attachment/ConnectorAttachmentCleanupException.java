/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.exception.attachment;

import eu.ecodex.connector.application.service.attachment.ConnectorAttachmentFilesDeleter;
import lombok.Getter;

/**
 * Exception thrown when the content of some attachments of a message could not be deleted during an
 * attachment cleanup.
 *
 * <p>It is raised by the cleanup of a message's attachments (see
 * {@link ConnectorAttachmentFilesDeleter}) when at least one attachment could not be removed from
 * the file storage. The attachments whose content was deleted are still marked as deleted; the
 * others stay active, so that a later run can retry them. It reports the message and the number of
 * attachments that failed, so that callers can log it or decide whether to retry.
 */
@Getter
public class ConnectorAttachmentCleanupException extends RuntimeException {
    private final String messageIdentifier;
    private final int failedCount;

    /**
     * Creates the exception.
     *
     * @param messageIdentifier the unique identifier of the message whose attachments could not all
     *                          be deleted
     * @param failedCount       the number of attachments of the message whose content could not be
     *                          deleted
     */
    public ConnectorAttachmentCleanupException(String messageIdentifier, int failedCount) {
        super("Could not delete [%d] attachment(s) of message [%s]"
                  .formatted(failedCount, messageIdentifier));
        this.messageIdentifier = messageIdentifier;
        this.failedCount = failedCount;
    }
}
