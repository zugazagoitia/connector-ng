/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.port.api.message;

import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import jakarta.annotation.Nonnull;
import java.time.Instant;

/**
 * Use case for cleaning up unresolved messages.
 *
 * <p>A message is unresolved when it has not reached a final state, that is, it is still waiting
 * for a transport or processing outcome (for example an acknowledgement or an evidence) after a
 * long time. Such a message will not be processed further, so its data is removed: the files of its
 * attachments and business content are deleted from the file storage, and the message is then
 * marked as deleted (see {@link ConnectorMessageRepository#setAsDeleted}). See
 * {@link ConnectorCleanupResolvedMessageAttachments} for the cleanup of the attachments of messages
 * that did reach a final state.
 */

public interface ConnectorCleanupUnresolvedMessages {
    /**
     * Cleans up the unresolved messages that have not been resolved since the given threshold.
     *
     * <p>The threshold protects recent messages that may still be resolved normally: only
     * unresolved messages created before it are handled (see
     * {@link ConnectorMessageRepository#findAllUnresolved}). For each of them, the stored content
     * of its attachments is removed (see {@link ConnectorFileStorageProvider#deleteByIdentifier}),
     * the attachments and business content are removed or marked as deleted, and the message is
     * marked as deleted. A failure on one message does not stop the cleanup of the others: the
     * failed message is left unresolved and picked up again by the next run.
     *
     * @param threshold the instant before which unresolved messages are cleaned up; messages
     *                  created at or after this instant are left untouched (must not be null)
     */
    void execute(@Nonnull Instant threshold);
}
