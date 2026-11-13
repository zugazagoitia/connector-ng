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

import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentDeletedException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.application.port.api.attachment.ConnectorDownloadAttachment;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import lombok.NonNull;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ConnectorDownloadAttachment}.
 */
@Service
public class ConnectorDownloadAttachmentService implements ConnectorDownloadAttachment {
    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorFileStorageProvider storageProvider;

    public ConnectorDownloadAttachmentService(
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorFileStorageProvider storageProvider) {
        this.attachmentRepository = attachmentRepository;
        this.storageProvider = storageProvider;
    }

    @Override
    public byte[] execute(@NonNull String identifier) {
        var attachment = this.attachmentRepository.findByIdentifier(identifier);

        if (attachment == null) {
            throw new ConnectorMessageAttachmentNotFoundException(
                "Attachment %s not found".formatted(identifier)
            );
        }

        if (attachment.deletedAt() != null) {
            throw new ConnectorMessageAttachmentDeletedException(
                "File [%s] has been deleted and its content is no longer available"
                    .formatted(identifier)
            );
        }

        var document = this.storageProvider.findByIdentifier(attachment.identifier());

        if (document == null) {
            throw new ConnectorMessageAttachmentException(
                ("Content of attachment %s is missing from the %s storage although "
                    + "the attachment is not marked as deleted")
                    .formatted(identifier, storageProvider.getStorage())
            );
        }

        return document;
    }
}
