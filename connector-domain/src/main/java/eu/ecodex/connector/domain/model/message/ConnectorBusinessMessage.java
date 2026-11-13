/*
 * Copyright 2025 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.domain.model.message;

import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

/**
 * Represents the main structure of a connector message within the system.
 *
 * <p>This record encapsulates the complete state of a message flowing within the system, which
 * includes its identifiers, associated metadata, direction, timestamps, and businessContent. It is
 * used for tracking, auditing, processing, and transforming messages as they transition between the
 * backend and the gateway.
 *
 * @param businessDomainIdentifier            The uuid for the business domain where the message is
 *                                            being used.
 * @param identifier                          A mandatory unique uuid for the message within the
 *                                            system scope.
 * @param backendMessageIdentifier            A mandatory unique uuid for the message from the
 *                                            backend perspective.
 * @param backendName                         The name of the backend system. Maybe null.
 * @param gatewayName                         The name of the gateway associated with this message.
 *                                            Maybe null.
 * @param as4Properties                       The AS4-specific metadata and configuration associated
 *                                            with the message.
 * @param direction                           The direction of message flow between the backend
 *                                            system and the gateway.
 * @param createdAt                           The timestamp indicating when the message was first
 *                                            created. Maybe null.
 * @param updatedAt                           The timestamp indicating the most recent update to the
 *                                            message. Maybe null.
 * @param deletedAt                           The timestamp indicating when the message was
 *                                            logically deleted. Maybe null.
 * @param rejectedAt                          The timestamp indicating when the message was rejected
 *                                            during processing. Maybe null.
 * @param confirmedAt                         The timestamp indicating when the backend or gateway
 *                                            confirmed the message. Maybe null.
 * @param deliveredToLinkPartnerAt            The timestamp indicating when the message was
 *                                            delivered to the link partner.
 * @param businessContent                     The core businessContent of the message, encapsulated
 *                                            as XML data and potentially a printable
 *                                            businessDocument. This field determines if the message
 *                                            is considered a business message.
 * @param attachments                         A list of attachment files associated with the
 *                                            message. Maybe null.
 * @param errors                              A list of errors, if any, encountered during message
 *                                            processing. Maybe null.
 * @param evidences                           A list of evidences associated with the message,
 *                                            aiding in auditing and validation. Maybe null.
 * @param transportedEvidences                A subset of evidences that were transported alongside
 *                                            the message, potentially during AS4 transmission.
 *                                            Maybe null.
 */
@Builder(toBuilder = true)
public record ConnectorBusinessMessage(
    // TODO check if caused by should be set to connector message definition
    @Nonnull ConnectorBusinessDomainIdentifier businessDomainIdentifier,
    @Nonnull String identifier,
    @Nullable String backendMessageIdentifier,
    @Nullable String backendName,
    @Nullable String gatewayName,
    @Nonnull ConnectorMessageAS4Properties as4Properties,
    @Nonnull ConnectorMessageDirection direction,
    @Nonnull ConnectorBusinessMessageStatus status,
    @Nullable Instant createdAt,
    @Nullable Instant updatedAt,
    @Nullable Instant deletedAt,
    @Nullable Instant rejectedAt,
    @Nullable Instant confirmedAt,
    @Nullable Instant deliveredToLinkPartnerAt,
    @NonNull ConnectorMessageBusinessContent businessContent,
    @Nullable List<ConnectorMessageAttachment> attachments,
    @Nullable List<ConnectorMessageError> errors,
    @Nullable List<ConnectorMessageEvidence> evidences,
    @Nullable List<ConnectorMessageEvidence> transportedEvidences
) implements Serializable, ConnectorMessage {
    /**
     * Initializes the message with default values for optional fields.
     */
    public ConnectorBusinessMessage {
        attachments = attachments == null ? List.of() : attachments;
        errors = errors == null ? List.of() : errors;
        evidences = evidences == null ? List.of() : evidences;
        transportedEvidences = transportedEvidences == null ? List.of() : transportedEvidences;
    }

    /**
     * Determines whether the current message has been rejected.
     *
     * <p>A message is considered rejected if the `rejectedAt` timestamp
     * is not null, indicating that rejection has been recorded for the message.
     *
     * @return {@code true} if the message is rejected; {@code false} otherwise.
     */
    public boolean isRejected() {
        return this.rejectedAt() != null;
    }

    @Override
    @Nonnull
    public String toString() {
        return String.format(
            "{identifier=%s, backendMessageIdentifier=%s, backendName=%s, gatewayName=%s, "
                + "direction=%s, as4Properties=%s businessContent=%s, attachments=%s, errors=%s, "
                + "evidences=%s",
            identifier, backendMessageIdentifier, backendName, gatewayName,
            direction, as4Properties, businessContent,
            attachments, errors, evidences
        );
    }
}
