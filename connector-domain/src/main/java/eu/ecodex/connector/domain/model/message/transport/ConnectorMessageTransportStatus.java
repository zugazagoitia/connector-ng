/*
 * Copyright 2025 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.domain.model.message.transport;

import lombok.Getter;

/**
 * Represents the various states of the message transport process within the connector system.
 *
 * <p>Each enumerated value corresponds to a specific phase or outcome of a message's journey
 * through the transport lifecycle. These statuses enable tracking and auditing of messages as they
 * are processed within the system.
 *
 * <ul>
 * <li><strong>DELIVERED</strong>: Indicates that the message has successfully been delivered
 * to the recipient (e.g., backend or gateway).</li>
 * <li><strong>FAILED</strong>: Indicates that the message could not be delivered due to
 * an error or system failure.</li>
 * <li><strong>SUBMITTED</strong>: Indicates that the message has been submitted to the gateway
 * system for further processing.</li>
 * <li><strong>PULLED</strong>: Indicates that the recipient or backend system has retrieved
 * or pulled the message.</li>
 * <li><strong>READY_FOR_PULL</strong>: Indicates that the message is available for the
 * recipient or backend system to be pulled.</li>
 * </ul>
 *
 * <p>Each status is associated with a predefined priority, represented as an integer value.
 * This priority can be used to determine the relative importance or order of processing
 * between different statuses.
 */
@Getter
public enum ConnectorMessageTransportStatus {
    DELIVERED(10), // when the message has been delivered to the recipient (backend/gateway)
    FAILED(10), // when the message could not be delivered
    SUBMITTED(9), // when the message has been submitted to the gateway system
    PULLED(9), // when the backend system has pulled the message
    READY_FOR_PULL(1); // when the message is ready to be pulled by the backend system

    final int priority;

    ConnectorMessageTransportStatus(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, priority=%s}", name(), priority);
    }
}
