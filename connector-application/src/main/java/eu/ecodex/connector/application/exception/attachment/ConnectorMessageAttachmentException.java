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

/**
 * Exception thrown when a file storage operation fails within the connector storage layer.
 *
 * <p>This exception represents unrecoverable errors that occur during file persistence operations,
 * such as I/O failures, storage provider errors, invalid storage responses, or configuration
 * issues.
 *
 * <p>It is a {@link RuntimeException} to allow propagation without mandatory catching, typically
 * handled at the application boundary (e.g. service layer, REST controller, or all exception
 * handler).
 */
public class ConnectorMessageAttachmentException extends RuntimeException {
    public ConnectorMessageAttachmentException(String message) {
        super(message);
    }
}
