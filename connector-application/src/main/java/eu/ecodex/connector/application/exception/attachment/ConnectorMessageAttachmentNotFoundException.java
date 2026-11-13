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

import eu.ecodex.connector.application.exception.NotFoundException;

/**
 * Exception thrown when a requested attachment for a connector message cannot be found.
 *
 * <p>This is a specialized form of {@link NotFoundException} that provides specific context for
 * scenarios where an operation fails because the required message attachment is not available.
 *
 * <p>Typical use cases for this exception include situations where the absence of an expected
 * attachment prevents the successful processing or handling of a connector message.
 */
public class ConnectorMessageAttachmentNotFoundException extends NotFoundException {
    public ConnectorMessageAttachmentNotFoundException(String message) {
        super(message);
    }
}
