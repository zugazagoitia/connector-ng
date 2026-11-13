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

import eu.ecodex.connector.application.port.api.message.ConnectorCleanupUnresolvedMessages;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.service.attachment.ConnectorAttachmentFilesDeleter;
import java.time.Instant;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ConnectorCleanupUnresolvedMessages}.
 */
@Slf4j
@Service
public class ConnectorCleanupUnresolvedMessagesService
    implements ConnectorCleanupUnresolvedMessages {
    private final ConnectorMessageRepository messageRepository;
    private final ConnectorAttachmentFilesDeleter filesDeleter;

    /**
     * Creates the service.
     *
     * @param messageRepository the repository used to find and delete the unresolved messages (must
     *                          not be null)
     * @param filesDeleter      the component that deletes the content of the attachments from the
     *                          file storage (must not be null)
     */
    public ConnectorCleanupUnresolvedMessagesService(
        ConnectorMessageRepository messageRepository,
        ConnectorAttachmentFilesDeleter filesDeleter) {
        this.messageRepository = messageRepository;
        this.filesDeleter = filesDeleter;
    }

    @Override
    public void execute(@NonNull Instant threshold) {
        var unresolvedMessages = messageRepository.findAllUnresolved(threshold);
        if (unresolvedMessages.isEmpty()) {
            log.debug("No unresolved messages older than {}", threshold);
            return;
        }
        log.info(
            "{} unresolved messages older than {} to process",
            unresolvedMessages.size(), threshold
        );

        var filesTotal = ConnectorAttachmentFilesDeleter.Result.NONE;
        var messagesDeleted = 0;
        var messagesFailed = 0;

        for (var message : unresolvedMessages) {
            var identifier = message.identifier();
            try {
                var result = filesDeleter.deleteActiveAttachmentsOf(identifier);
                filesTotal = filesTotal.plus(result);

                if (result.failed() > 0) {
                    // keep the message and its attachment rows so the next run retries the files
                    messagesFailed++;
                    continue;
                }

                messageRepository.deleteByIdentifier(identifier);
                messagesDeleted++;
            } catch (RuntimeException e) {
                // e.g. database error: do not abort the other messages, retried at next run
                messagesFailed++;
                log.warn("Could not delete unresolved message [{}]", identifier, e);
            }
        }

        log.info(
            "Unresolved messages cleanup done: [{}] messages deleted, [{}] failed; "
                + "files: [{}] deleted, [{}] failed",
            messagesDeleted, messagesFailed, filesTotal.deleted(), filesTotal.failed()
        );
    }
}
