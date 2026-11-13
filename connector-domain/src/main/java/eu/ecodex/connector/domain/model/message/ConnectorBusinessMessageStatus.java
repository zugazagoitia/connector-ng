/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.domain.model.message;

import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import java.util.stream.Stream;
import lombok.NonNull;


/**
 * Status of a connector business message, covering its life cycle from creation to its final
 * state.
 *
 * <p>The statuses combine the progress of the transport of the message (submission, delivery,
 * pulling) with its resolution (confirmed, rejected) and its deletion. The final states are
 * {@link #CONFIRMED}, {@link #REJECTED}, {@link #FAILED} and {@link #DELETED}.
 */
public enum ConnectorBusinessMessageStatus {
    CREATED,
    FAILED,
    SUBMITTED,
    DELIVERED,
    CONFIRMED,
    REJECTED,
    DELETED,
    READY_FOR_PULL,
    RECEIVED_BY_OPPOSITE_CONNECTOR,
    PULLED;

    /**
     * Returns the status whose name equals the given transport step status.
     *
     * <p>The match is exact and case-sensitive, on the name of the constant. This allows a
     * transport status (see {@link ConnectorMessageTransportStatus}) that has the same name as a
     * message status to be converted to it.
     *
     * @param stepStatus the name of the transport step status to convert (must not be null)
     *
     * @return the matching {@link ConnectorBusinessMessageStatus}, or {@code null} if no status has
     *     this name
     */
    public static ConnectorBusinessMessageStatus fromTransportStep(@NonNull String stepStatus) {
        return Stream.of(values())
                     .filter(status -> status.name().equals(stepStatus))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException(
                         "Illegal transport step: [%s]".formatted(stepStatus)
                     ));
    }
}
