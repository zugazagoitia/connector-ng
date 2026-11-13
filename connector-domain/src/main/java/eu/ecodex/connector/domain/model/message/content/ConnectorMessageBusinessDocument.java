/*
 * Copyright 2025 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.domain.model.message.content;

import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import lombok.Builder;

/**
 * Holds the printable business document of a message.
 *
 * <p>The content of the document is not held in this object: it is stored in an attachment (see
 * {@link ConnectorMessageAttachment}, which is read from the file storage). The record also holds
 * the optional detached signature of the document, which is another attachment, and the optional
 * AES encryption type applied to it.
 *
 * @param uuid              the unique identifier of the business document; may be {@code null} if
 *                          the document has not been persisted yet
 * @param content           the {@link ConnectorMessageAttachment} containing the payload of the
 *                          business document (must not be null)
 * @param detachedSignature the {@link ConnectorMessageAttachment} containing the detached signature
 *                          of the business document; may be {@code null} if the document has no
 *                          detached signature
 * @param aesType           the {@link ConnectorBusinessDocumentAESType} of the AES encryption
 *                          applied to the business document; may be {@code null} if the document is
 *                          not encrypted
 */

@Builder(toBuilder = true)
public record ConnectorMessageBusinessDocument(
    @Nullable String uuid,
    @Nonnull ConnectorMessageAttachment content,
    @Nullable ConnectorMessageAttachment detachedSignature,
    @Nullable ConnectorBusinessDocumentAESType aesType
) implements Serializable {
    @Override
    @Nonnull
    public String toString() {
        return String.format(
            "{content=%s, detachedSignature=%s, aesType=%s}",
            content, detachedSignature, aesType
        );
    }
}
