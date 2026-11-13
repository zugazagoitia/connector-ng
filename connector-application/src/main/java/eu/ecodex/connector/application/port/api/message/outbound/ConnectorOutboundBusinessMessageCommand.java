/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.port.api.message.outbound;

import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.message.ConnectorMessageAS4Properties;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.Builder;

/**
 * Represents a record for sending outbound business messages through the connector system.
 *
 * @param businessDomainIdentifier            Represents the unique identifier of the business
 *                                            domain associated with this message.
 * @param backendMessageIdentifier            The unique identifier of the message in the backend
 *                                            system. This value is optional and may not always be
 *                                            present.
 * @param as4Properties                       Encapsulates the AS4-specific properties related to
 *                                            the message, including sender, recipient, and routing
 *                                            details. This field is mandatory.
 * @param direction                           The direction of the message flow (e.g., backend to
 *                                            gateway or gateway to backend).
 * @param businessContent                     Contains the main business content of the message,
 *                                            including XML structures and associated documents.
 *                                            This field is required.
 * @param attachments                         A list of additional attachments associated with the
 *                                            message, such as supplementary documents or metadata.
 *                                            Defaults to an empty list if not provided.
 */
@Builder(toBuilder = true)
public record ConnectorOutboundBusinessMessageCommand(
    @Nonnull ConnectorBusinessDomainIdentifier businessDomainIdentifier,
    @Nullable String backendMessageIdentifier,
    @Nonnull String backendName,
    @Nonnull ConnectorMessageAS4Properties as4Properties,
    @Nonnull ConnectorMessageDirection direction,
    @Nonnull ConnectorMessageBusinessContent businessContent,
    @Nullable List<ConnectorMessageAttachment> attachments
) {
    public ConnectorOutboundBusinessMessageCommand {
        attachments = attachments == null ? List.of() : attachments;
    }
}
