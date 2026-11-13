/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.port.spi.message;

import eu.ecodex.connector.application.port.api.attachment.ConnectorUploadAttachments;
import eu.ecodex.connector.application.port.api.message.ConnectorMessageAttachmentLinker;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.domain.model.filter.ConnectorAttachmentsListFilter;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.List;

/**
 * Repository (port) for persisting and querying {@link ConnectorMessageAttachment} objects.
 *
 * <p>Attachments are documents, such as PDFs, that are sent as part of a message. The repository
 * stores and retrieves their metadata; the content itself is read from the file storage (see
 * {@code ConnectorFileStorageProvider}).
 */
public interface ConnectorMessageAttachmentRepository {
    /**
     * Persists the given attachment.
     *
     * <p>The attachment carries metadata such as its identifier, name, MIME type and description.
     *
     * @param attachment the attachment to save (must not be null)
     *
     * @return the saved {@link ConnectorMessageAttachment}, including any values generated during
     *     persistence, such as an identifier
     */
    ConnectorMessageAttachment save(ConnectorMessageAttachment attachment);

    /**
     * Retrieves an attachment by its unique identifier.
     *
     * @param identifier the unique identifier of the attachment (must not be null or blank)
     *
     * @return the {@link ConnectorMessageAttachment} matching the given identifier, or {@code null}
     *     if no such attachment exists
     */
    ConnectorMessageAttachment findByIdentifier(@Nonnull String identifier);

    /**
     * Verifies if any attachment exists and matched by it identifier.
     *
     * @param identifier the unique identifier to check
     *
     * @return true if any attachment matches this identifier
     */
    boolean existsByIdentifier(@Nonnull String identifier);

    /**
     * Retrieves all attachments of the message identified by the given identifier that have not
     * been marked as deleted, whatever their type.
     *
     * <p>Attachments marked as deleted (see {@link #markAsDeleted}) are excluded, so the returned
     * attachments are those whose content is expected to still be available. Unlike
     * {@link #findByMessageIdentifierAndTypes}, no type filter is applied.
     *
     * @param messageIdentifier the unique identifier of the message the attachments belong to (must
     *                          not be null)
     *
     * @return the active {@link ConnectorMessageAttachment} objects of the message; never
     *     {@code null}, and empty if the message has no active attachments or does not exist
     */
    List<ConnectorMessageAttachment> findAllActiveByMessageIdentifier(
        @Nonnull String messageIdentifier);

    /**
     * Retrieves the attachments of a message that have one of the given types.
     *
     * @param messageIdentifier identifier of the message the attachments belong to (must not be
     *                          null)
     * @param types             the attachment types to match (must not be null)
     *
     * @return the matching {@link ConnectorMessageAttachment} objects; never {@code null}, and
     *     empty if there are none
     */
    List<ConnectorMessageAttachment> findByMessageIdentifierAndTypes(
        @Nonnull String messageIdentifier, @Nonnull List<ConnectorAttachmentType> types);

    /**
     * Retrieves a page of {@link ConnectorMessageAttachment} objects based on the pagination
     * parameters and the optional filter criteria carried by the given filter. All filters are
     * combined with a logical {@code AND}; a filter whose value is {@code null} or empty is
     * ignored.
     *
     * @param filter the pagination parameters and filter criteria used to retrieve the attachments
     *               (must not be null)
     *
     * @return a {@link ConnectorPageResult} containing the attachments matching the filter for the
     *     requested page, along with pagination metadata such as the total number of elements
     */
    ConnectorPageResult<ConnectorMessageAttachment> findAll(
        @Nonnull ConnectorAttachmentsListFilter filter);

    /**
     * Retrieves the orphaned attachments created before the given threshold.
     *
     * <p>An attachment is orphaned when it was uploaded (see {@link ConnectorUploadAttachments})
     * but never linked to a message (see {@link ConnectorMessageAttachmentLinker}). Attachments
     * already marked as deleted (see {@link #markAsDeleted}) are not returned.
     *
     * @param threshold the instant before which orphaned attachments are returned; attachments
     *                  created at or after this instant are excluded (must not be null)
     *
     * @return the orphaned {@link ConnectorMessageAttachment} objects created before the threshold;
     *     never {@code null}, and empty if there are none
     */
    List<ConnectorMessageAttachment> findAllOrphaned(Instant threshold);

    /**
     * Associates an attachment with a message.
     *
     * <p>The link enables the attached document to be retrieved or referenced in relation to the
     * message.
     *
     * @param attachmentIdentifier the unique identifier of the attachment to link (must not be null
     *                             or blank)
     * @param messageIdentifier    the unique identifier of the message the attachment is associated
     *                             with (must not be null or blank)
     */
    void attachToMessage(@Nonnull String attachmentIdentifier, @Nonnull String messageIdentifier);

    /**
     * Updates the type of attachment.
     *
     * @param attachmentIdentifier the unique identifier of the attachment to update (must not be
     *                             null)
     * @param type                 the new {@link ConnectorAttachmentType} (must not be null)
     */
    void updateType(@Nonnull String attachmentIdentifier, @Nonnull ConnectorAttachmentType type);

    /**
     * Marks the attachments identified by the given identifiers as deleted, recording the instant
     * of the deletion.
     *
     * <p>This is a soft delete: the attachments are flagged as deleted at the given instant rather
     * than removed from the repository.
     *
     * @param identifiers the unique identifiers of the attachments to mark as deleted (must not be
     *                    null); if the list is empty, nothing is updated
     * @param deletedAt   the instant at which the attachments were deleted (must not be null)
     */
    void markAsDeleted(List<String> identifiers, Instant deletedAt);

    /**
     * Permanently deletes the attachments identified by the given identifiers.
     *
     * <p>Unlike {@link #markAsDeleted}, which only flags attachments as deleted, this physically
     * removes the attachment rows. Identifiers that match no attachment are ignored, and nothing
     * happens if the list is empty. Only the metadata is removed: the content of the attachments in
     * the file storage is not affected (see
     * {@link ConnectorFileStorageProvider#deleteByIdentifier}).
     *
     * @param identifiers the unique identifiers of the attachments to delete (must not be null)
     */
    void deleteAllByIdentifiers(@Nonnull List<String> identifiers);
}
