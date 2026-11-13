/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto;

import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import java.time.Instant;
import lombok.Builder;

/**
 * Data Transfer Object representing an attachment managed by the connector.
 *
 * <p>This record encapsulates metadata describing an attachment, including
 * its identifier, file information, storage location, and lifecycle timestamps. It does not contain
 * the binary content itself.
 *
 * @param identifier  the unique identifier of the attachment
 * @param name        the original or display name of the attachment
 * @param contentType the MIME type of the attachment (e.g. {@code application/pdf})
 * @param size        the size of the attachment in bytes
 * @param description an optional human-readable description of the attachment
 * @param storage     the storage strategy or location used to persist the attachment
 * @param type        the type of the attachment (e.g. {@code BUSINESS_DOCUMENT})
 * @param createdAt   the timestamp when the attachment was created
 * @param updatedAt   the timestamp when the attachment was last updated
 * @param deletedAt   the deletion date of the attachment.
 */
@Builder
public record ConnectorAttachmentDto(
    String identifier,
    String name,
    String contentType,
    long size,
    String description,
    ConnectorAttachmentStorage storage,
    ConnectorAttachmentType type,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt
) {
    /**
     * Creates an instance of {@code ConnectorAttachmentDto} from a given
     * {@code ConnectorMessageAttachment}.
     *
     * @param attachment the {@code ConnectorMessageAttachment} containing the details to populate
     *                   the DTO
     *
     * @return a new instance of {@code ConnectorAttachmentDto} populated with the properties from
     *     the provided attachment
     */
    public static ConnectorAttachmentDto from(ConnectorMessageAttachment attachment) {
        return ConnectorAttachmentDto
            .builder()
            .identifier(attachment.identifier())
            .name(attachment.name())
            .size(attachment.size())
            .contentType(attachment.contentType())
            .description(attachment.description())
            .storage(attachment.storage())
            .type(attachment.type())
            .createdAt(attachment.createdAt())
            .updatedAt(attachment.updatedAt())
            .deletedAt(attachment.deletedAt())
            .build();
    }
}
