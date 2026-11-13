/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message;

import eu.ecodex.connector.application.exception.attachment.ConnectorAttachmentCleanupException;
import eu.ecodex.connector.application.port.api.message.ConnectorCleanupResolvedMessageAttachments;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.service.attachment.ConnectorAttachmentFilesDeleter;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageResolvedEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ConnectorCleanupResolvedMessageAttachments}.
 *
 * <p>The service removes the attachments of a message that has been resolved (see
 * {@link ConnectorMessageResolvedEvent}) by delegating to a
 * {@link ConnectorAttachmentFilesDeleter}, which deletes their content from the file storage and
 * marks them as deleted.
 */
@Slf4j
@Service
public class ConnectorCleanupResolvedMessageAttachmentsService implements
    ConnectorCleanupResolvedMessageAttachments {
    private final ConnectorAttachmentFilesDeleter filesDeleter;
    private final ConnectorMessageRepository messageRepository;

    /**
     * Creates the service.
     *
     * @param filesDeleter the component that deletes the content of the attachments from the file
     *                     storage and marks them as deleted (must not be null)
     */
    public ConnectorCleanupResolvedMessageAttachmentsService(
        ConnectorAttachmentFilesDeleter filesDeleter,
        ConnectorMessageRepository messageRepository) {
        this.filesDeleter = filesDeleter;
        this.messageRepository = messageRepository;
    }

    @Override
    public void execute(@NonNull String messageIdentifier) {
        var result = filesDeleter.deleteActiveAttachmentsOf(messageIdentifier);

        if (result.failed() > 0) {
            // The successful deletions are already marked. Throwing makes the JMS listener
            // redeliver the message, and the retry only processes the remaining attachments.
            throw new ConnectorAttachmentCleanupException(messageIdentifier, result.failed());
        }

        messageRepository.setAsDeleted(messageIdentifier);
        messageRepository.updateStatus(messageIdentifier, ConnectorBusinessMessageStatus.DELETED);

        log.info(
            "Resolved message [{}]: [{}] attachments deleted",
            messageIdentifier, result.deleted()
        );
    }
}
