/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.port.spi;

import eu.ecodex.connector.application.port.api.attachment.ConnectorCleanupOrphanedAttachments;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import jakarta.annotation.Nonnull;
import java.nio.file.Path;

/**
 * Provider (port) for storing and retrieving the content of connector message attachments in an
 * underlying storage system, such as a file system, a database or an object store.
 */
public interface ConnectorFileStorageProvider {
    /**
     * Saves the content of the given attachment, read from a file on the local filesystem, to the
     * underlying storage system.
     *
     * @param attachment the {@link ConnectorMessageAttachment} metadata associated with the file
     *                   being stored (must not be null)
     * @param filePath   the path of the file on the local filesystem whose content is stored (must
     *                   not be null)
     *
     * @return the storage reference of the saved file (for example an object key, a path or a URL)
     */
    String save(@Nonnull ConnectorMessageAttachment attachment, @Nonnull Path filePath);

    /**
     * Saves the given attachment content, provided as a byte array, to the underlying storage
     * system.
     *
     * @param attachment the {@link ConnectorMessageAttachment} metadata associated with the content
     *                   being stored (must not be null)
     * @param content    the content to store (must not be null)
     *
     * @return the storage reference of the saved content (for example an object key, a path or a
     *     URL)
     */
    String save(@Nonnull ConnectorMessageAttachment attachment, @Nonnull byte[] content);

    /**
     * Retrieves the content of an attachment from the underlying storage system.
     *
     * @param identifier the identifier of the attachment whose content is retrieved (must not be
     *                   null)
     *
     * @return the content of the attachment as a byte array, or {@code null} if no content is
     *     stored for the given identifier
     */
    byte[] findByIdentifier(@Nonnull String identifier);

    /**
     * Returns the type of storage handled by this provider.
     *
     * @return the {@link ConnectorAttachmentStorage} identifying this provider's storage mode
     */
    ConnectorAttachmentStorage getStorage();

    /**
     * Deletes the content of an attachment from the underlying storage system.
     *
     * <p>Only the stored content is removed; the attachment metadata is handled separately (see
     * {@link ConnectorMessageAttachmentRepository#markAsDeleted}). This is used, for example, by
     * {@link ConnectorCleanupOrphanedAttachments} to remove the content of orphaned attachments.
     *
     * @param identifier the identifier of the attachment whose content is deleted (must not be
     *                   null)
     */
    void deleteByIdentifier(@Nonnull String identifier);
}
