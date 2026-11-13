/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.port.api.transport.command;

import eu.ecodex.connector.domain.model.message.ConnectorMessageError;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import jakarta.annotation.Nonnull;
import java.util.List;
import lombok.Builder;

/**
 * Command carrying the information needed to update the transport status of a connector message.
 *
 * @param remoteMessageIdentifier identifier of the message as known by the remote system, for
 *                                example the message identifier assigned by a backend system; may
 *                                be {@code null} if the remote system did not assign one
 * @param status                  the new transport status ({@link ConnectorMessageTransportStatus})
 *                                to record for the message
 * @param linkPartnerName         name of the link partner updating the message
 * @param errors                  the errors ({@link ConnectorMessageError}) associated with the
 *                                transport, for example when the status denotes a failure; may be
 *                                {@code null} or empty if there are none
 */
@Builder(toBuilder = true)
public record UpdateMessageTransportCommand(
    // e.g.: backend system message id
    String remoteMessageIdentifier,
    @Nonnull String linkPartnerName,
    @Nonnull ConnectorMessageTransportStatus status,
    List<ConnectorMessageError> errors
) {
}
