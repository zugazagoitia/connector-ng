/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.message;

import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageAS4Properties;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

/**
 * Represents a Data Transfer Object (DTO) for a connector message.
 *
 * <p>This record encapsulates metadata and lifecycle management-related timestamps
 * for a message transmitted between a backend system and a gateway. It includes properties for
 * identifying business domains, backend systems, and gateways, as well as directional and
 * AS4-specific message properties.
 *
 * <p>This DTO is primarily used for tracking, processing, auditing, and managing
 * the state of messages.
 *
 * @param businessDomainIdentifier The identifier for the business domain associated with the
 *                                 message.
 * @param identifier               The unique identifier for this message.
 * @param backendMessageIdentifier The identifier for the message in the backend system.
 * @param backendName              The name of the backend system processing this message.
 * @param gatewayName              The name of the gateway involved in processing this message.
 * @param as4Properties            AS4-specific properties associated with this message.
 * @param direction                The direction of the message flow, either from the backend to the
 *                                 gateway or vice versa.
 * @param status                   current {@link ConnectorBusinessMessageStatus} of the message
 * @param delivered                A flag indicating whether the message has been delivered
 * @param createdAt                The creation timestamp for the message.
 * @param updatedAt                The timestamp when the message was last updated.
 * @param deletedAt                The timestamp when the message was deleted.
 * @param rejectedAt               The timestamp when the message was rejected.
 * @param confirmedAt              The timestamp when the message was confirmed.
 * @param deliveredToLinkPartnerAt The timestamp when the message was delivered to the link
 *                                 partner.
 */
@Builder
public record ConnectorMessageDto(
    String businessDomainIdentifier,
    String identifier,
    String backendMessageIdentifier,
    String backendName,
    String gatewayName,
    ConnectorMessageAS4Properties as4Properties,
    ConnectorMessageDirection direction,
    ConnectorBusinessMessageStatus status,
    boolean delivered,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt,
    Instant rejectedAt,
    Instant confirmedAt,
    Instant deliveredToLinkPartnerAt
) {
    /**
     * Converts a {@link ConnectorBusinessMessage} instance into a {@link ConnectorMessageDto}
     * instance.
     *
     * @param message the {@link ConnectorBusinessMessage} to be converted
     *
     * @return a new {@link ConnectorMessageDto} instance containing the mapped values
     */
    public static ConnectorMessageDto from(ConnectorBusinessMessage message) {
        return ConnectorMessageDto
            .builder()
            .businessDomainIdentifier(message.businessDomainIdentifier().messageLaneIdentifier())
            .identifier(message.identifier())
            .backendMessageIdentifier(message.backendMessageIdentifier())
            .direction(Objects.requireNonNull(message.direction()))
            .status(message.status())
            .delivered(isDelivered(message))
            .backendName(message.backendName())
            .gatewayName(message.gatewayName())
            .as4Properties(message.as4Properties())
            .createdAt(message.createdAt())
            .updatedAt(message.updatedAt())
            .deletedAt(message.deletedAt())
            .rejectedAt(message.rejectedAt())
            .confirmedAt(message.confirmedAt())
            .deliveredToLinkPartnerAt(message.deliveredToLinkPartnerAt())
            .build();
    }

    private static boolean isDelivered(ConnectorBusinessMessage message) {
        return message.deliveredToLinkPartnerAt() != null;
    }
}
