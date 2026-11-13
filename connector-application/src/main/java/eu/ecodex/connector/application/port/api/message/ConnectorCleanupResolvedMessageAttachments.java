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

import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.ConnectorMessageResolvedEvent;
import jakarta.annotation.Nonnull;

/**
 * Use case for cleaning up the attachments of a resolved message.
 *
 * <p>A message is resolved when it has reached a final state, that is, confirmed or rejected (see
 * {@link ConnectorMessageResolvedEvent}). Once resolved, the message will not be processed further,
 * so its attachments are no longer needed in the file storage. See
 * {@link ConnectorCleanupUnresolvedMessages} for the cleanup of messages that never get
 * resolved.
 */
public interface ConnectorCleanupResolvedMessageAttachments {
    /**
     * Cleans up the attachments of the resolved message identified by the given identifier.
     *
     * <p>The content of the attachments is removed from the file storage and the attachments are
     * marked as deleted (see {@link ConnectorMessageAttachmentRepository#markAsDeleted}).
     *
     * @param messageIdentifier the unique identifier of the resolved message whose attachments are
     *                          cleaned up (must not be null)
     */
    void execute(@Nonnull String messageIdentifier);
}
