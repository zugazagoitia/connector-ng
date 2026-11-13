/*
 * Copyright 2025 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.domain.model.message.attachment;

import jakarta.annotation.Nonnull;
import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;

/**
 * This object contains an attachment for a message. With every message there can be some documents
 * sent along with. Therefore, those documents are attached to the message over this type.
 *
 * @param identifier  The identifier of the attachment.
 * @param name        The name of the attachment.
 * @param contentType The MIME type of the attachment.
 * @param size        The size of the attachment
 * @param description A description of the attachment.
 * @param storage     The message storage type.
 * @param type        The type of the attachment.
 * @param createdAt   The creation date of the attachment.
 * @param updatedAt   The last update date of the attachment.
 * @param deletedAt   The deletion date of the attachment.
 */
@Builder(toBuilder = true)
public record ConnectorMessageAttachment(
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
) implements Serializable {
    @Override
    @Nonnull
    public String toString() {
        return String.format(
            "{identifier=%s, name=%s, contentType=%s, description=%s, storage=%s, type=%s}",
            identifier, name, contentType, description, storage, type
        );
    }
}
