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
import eu.ecodex.connector.domain.model.message.ConnectorEvidenceMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageAS4Properties;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.ConnectorMessageError;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;

/**
 * Detailed representation of a connector message exchanged between a backend system and a gateway.
 *
 * @param businessDomainIdentifier identifier of the business domain to which the message belongs
 * @param identifier               unique identifier of the connector message
 * @param backendMessageIdentifier identifier assigned to the message by the backend system
 * @param backendName              name of the backend system associated with the message
 * @param gatewayName              name of the gateway responsible for transmitting or receiving the
 *                                 message
 * @param businessContent          business content of the message
 * @param as4Properties            AS4-specific metadata and properties associated with the message
 * @param direction                direction of the message flow (e.g., inbound or outbound)
 * @param status                   current {@link ConnectorBusinessMessageStatus} of the message
 * @param isBusiness               indicates whether the message is a business message
 * @param createdAt                timestamp when the message was created
 * @param updatedAt                timestamp when the message was last updated
 * @param deletedAt                timestamp when the message was deleted
 * @param rejectedAt               timestamp when the message was rejected
 * @param confirmedAt              timestamp when the message was confirmed
 * @param attachments              list of attachments associated with the message
 * @param evidences                list of evidences related to message processing or delivery
 * @param errors                   list of processing or delivery errors associated with the
 *                                 message
 */
@Builder
public record ConnectorMessageDetailDto(
    String businessDomainIdentifier,
    String identifier,
    String backendMessageIdentifier,
    String backendName,
    String gatewayName,
    ConnectorMessageBusinessContent businessContent,
    ConnectorMessageAS4Properties as4Properties,
    ConnectorMessageDirection direction,
    ConnectorBusinessMessageStatus status,
    boolean isBusiness,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt,
    Instant rejectedAt,
    Instant confirmedAt,
    Instant deliveredToLinkPartnerAt,
    List<ConnectorMessageAttachment> attachments,
    List<ConnectorMessageEvidenceDto> evidences,
    List<ConnectorMessageError> errors
) {
    private static final Set<ConnectorAttachmentType> INBOUND_INCLUDED_ATTACHMENT_TYPES =
        EnumSet.of(
            ConnectorAttachmentType.ATTACHMENT,
            ConnectorAttachmentType.PDF_TOKEN,
            ConnectorAttachmentType.XML_TOKEN
        );

    private static final Set<ConnectorAttachmentType> OUTBOUND_INCLUDED_ATTACHMENT_TYPES =
        EnumSet.of(ConnectorAttachmentType.ATTACHMENT);

    /**
     * Converts a {@link ConnectorBusinessMessage} object into a {@link ConnectorMessageDetailDto}.
     *
     * @param message the {@link ConnectorBusinessMessage} object to be converted
     *
     * @return a newly created {@link ConnectorMessageDetailDto} based on the values of the provided
     *     {@link ConnectorBusinessMessage}
     */
    public static ConnectorMessageDetailDto from(ConnectorBusinessMessage message) {
        return ConnectorMessageDetailDto
            .builder()
            .businessDomainIdentifier(
                message.businessDomainIdentifier().messageLaneIdentifier())
            .identifier(message.identifier())
            .backendMessageIdentifier(message.backendMessageIdentifier())
            .direction(Objects.requireNonNull(message.direction()))
            .status(message.status())
            .isBusiness(true)
            .backendName(message.backendName())
            .gatewayName(message.gatewayName())
            .businessContent(message.businessContent())
            .as4Properties(message.as4Properties())
            .createdAt(message.createdAt())
            .updatedAt(message.updatedAt())
            .deletedAt(message.deletedAt())
            .rejectedAt(message.rejectedAt())
            .confirmedAt(message.confirmedAt())
            .deliveredToLinkPartnerAt(message.deliveredToLinkPartnerAt())
            .errors(message.errors())
            .attachments(filterAttachments(message))
            .evidences(toEvidences(message.evidences()))
            .build();
    }

    /**
     * Converts a {@link ConnectorEvidenceMessage} instance into a {@link ConnectorMessageDetailDto}
     * instance.
     *
     * @param message     the {@link ConnectorEvidenceMessage} to be converted
     * @param deliveredAt the instant at which the message was delivered; may be {@code null} if the
     *                    message has not been delivered yet
     *
     * @return a new {@link ConnectorMessageDto} instance containing the mapped values
     */
    public static ConnectorMessageDetailDto from(
        ConnectorEvidenceMessage message,
        Instant deliveredAt) {
        return ConnectorMessageDetailDto
            .builder()
            .businessDomainIdentifier(
                message.businessDomainIdentifier().messageLaneIdentifier())
            .identifier(message.identifier())
            .backendMessageIdentifier(message.backendMessageIdentifier())
            .direction(Objects.requireNonNull(message.direction()))
            .backendName(message.backendName())
            .gatewayName(message.gatewayName())
            .as4Properties(message.as4Properties())
            .createdAt(null)
            .updatedAt(null)
            .deletedAt(null)
            .rejectedAt(null)
            .confirmedAt(null)
            .deliveredToLinkPartnerAt(deliveredAt)
            .evidences(toEvidences(message.transportedEvidences()))
            .build();
    }

    private static List<ConnectorMessageEvidenceDto> toEvidences(
        List<ConnectorMessageEvidence> evidences) {
        if (evidences == null) {
            return List.of();
        }

        return evidences.stream()
                        .map(ConnectorMessageEvidenceDto::from)
                        .toList();
    }

    private static List<ConnectorMessageAttachment> filterAttachments(
        ConnectorBusinessMessage message) {
        var attachments = message.attachments();

        if (attachments == null) {
            return List.of();
        }

        var includedTypes = message.direction() == ConnectorMessageDirection.GATEWAY_TO_BACKEND
            ? INBOUND_INCLUDED_ATTACHMENT_TYPES
            : OUTBOUND_INCLUDED_ATTACHMENT_TYPES;

        return attachments.stream()
                          .filter(att -> includedTypes.contains(att.type()))
                          .toList();
    }
}
