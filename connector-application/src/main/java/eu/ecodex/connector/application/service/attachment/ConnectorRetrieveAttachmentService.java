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

import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.application.port.api.attachment.ConnectorRetrieveAttachment;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import lombok.NonNull;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ConnectorRetrieveAttachment}.
 */
@Service
public class ConnectorRetrieveAttachmentService implements ConnectorRetrieveAttachment {
    private final ConnectorMessageAttachmentRepository attachmentRepository;

    public ConnectorRetrieveAttachmentService(
        ConnectorMessageAttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    public ConnectorMessageAttachment execute(@NonNull String identifier) {
        var attachment = this.attachmentRepository.findByIdentifier(identifier);

        if (attachment == null) {
            throw new ConnectorMessageAttachmentNotFoundException("Attachment not found");
        }

        return attachment;
    }
}
