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

import eu.ecodex.connector.application.port.api.attachment.ConnectorDownloadAttachment;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;

/**
 * Exception thrown when a requested attachment of a connector message has been deleted and its
 * content is therefore no longer available.
 *
 * <p>It is raised when an operation needs the content of an attachment that was marked as deleted
 * (see {@link ConnectorMessageAttachmentRepository#markAsDeleted}), for example when downloading it
 * through {@link ConnectorDownloadAttachment}. It is distinct from
 * {@link ConnectorMessageAttachmentNotFoundException}, which signals an attachment that never
 * existed or is unknown.
 *
 * <p>This is an unchecked exception.
 */
public class ConnectorMessageAttachmentDeletedException extends RuntimeException {
    /**
     * Creates the exception with the given detail message.
     *
     * @param message the detail message describing which attachment was deleted
     */
    public ConnectorMessageAttachmentDeletedException(String message) {
        super(message);
    }
}
