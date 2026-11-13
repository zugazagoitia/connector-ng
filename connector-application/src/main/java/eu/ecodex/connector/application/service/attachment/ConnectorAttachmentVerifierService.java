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

import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.application.port.api.attachment.ConnectorAttachmentVerifier;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import java.util.HashSet;
import java.util.List;
import lombok.NonNull;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ConnectorAttachmentVerifier}.
 */
@Service
public class ConnectorAttachmentVerifierService implements ConnectorAttachmentVerifier {
    private final ConnectorMessageAttachmentRepository attachmentRepository;

    public ConnectorAttachmentVerifierService(
        ConnectorMessageAttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    public void execute(@NonNull List<ConnectorMessageAttachment> attachments) {
        var seen = new HashSet<>();
        for (var attachment : attachments) {
            var identifier = attachment.identifier();

            if (!seen.add(identifier)) {
                throw new ConnectorMessageAttachmentException(
                    "Duplicate attachment identifier found: [%s] ".formatted(identifier));
            }

            var exists = this.attachmentRepository.existsByIdentifier(identifier);
            if (!exists) {
                throw new ConnectorMessageAttachmentNotFoundException(
                    "Attachment not found by identifier: [%s] ".formatted(identifier));
            }
        }
    }
}
