/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.attachment;

import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.application.service.message.ConnectorCleanupResolvedMessageAttachmentsService;
import eu.ecodex.connector.application.service.message.ConnectorCleanupUnresolvedMessagesService;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Deletes the files of attachments from the file storage and marks them as deleted.
 *
 * <p>This component holds the deletion logic shared by the attachment cleanups (see
 * {@link ConnectorCleanupOrphanedAttachmentsService},
 * {@link ConnectorCleanupUnresolvedMessagesService} and
 * {@link ConnectorCleanupResolvedMessageAttachmentsService}). For each attachment, the content is
 * removed from the file storage (see {@link ConnectorFileStorageProvider#deleteByIdentifier}), then
 * the attachments whose content was deleted are marked as deleted in the
 * {@link ConnectorMessageAttachmentRepository} (see
 * {@link ConnectorMessageAttachmentRepository#markAsDeleted}), using the {@link Clock} to record
 * the deletion instant.
 */
@Slf4j
@Service
public class ConnectorAttachmentFilesDeleter {
    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorFileStorageProvider fileStorageProvider;
    private final Clock clock;

    /**
     * Creates the deleter.
     *
     * @param attachmentRepository the repository used to update the attachments once their content
     *                             has been deleted (must not be null)
     * @param fileStorageProvider  the provider used to delete the content of the attachments from
     *                             the file storage (must not be null)
     * @param clock                the clock used to record the deletion instant, which allows a
     *                             fixed clock in tests (must not be null)
     */
    public ConnectorAttachmentFilesDeleter(
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorFileStorageProvider fileStorageProvider,
        Clock clock) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageProvider = fileStorageProvider;
        this.clock = clock;
    }

    /**
     * Deletes the files of the active (not yet deleted) attachments of a message, then marks them
     * as deleted.
     */
    public Result deleteActiveAttachmentsOf(@NonNull String messageIdentifier) {
        return delete(attachmentRepository.findAllActiveByMessageIdentifier(messageIdentifier));
    }

    /**
     * Deletes the files of the given attachments from the storage, then marks the successfully
     * deleted ones as deleted (soft delete: the rows are kept). A failure on one file does not stop
     * the others: the failed attachment is left untouched and picked up by the next run.
     */
    public Result delete(@NonNull List<ConnectorMessageAttachment> attachments) {
        var outcome = deleteFiles(attachments);
        if (!outcome.deletedIdentifiers().isEmpty()) {
            attachmentRepository.markAsDeleted(outcome.deletedIdentifiers(), clock.instant());
        }
        return outcome.toResult();
    }

    /**
     * Deletes the files of the given attachments from the storage, then permanently removes the
     * rows of the successfully deleted ones. A failure on one file does not stop the others: the
     * failed attachment row is kept so that the next run retries it.
     *
     * <p>If the row removal fails after the files are gone, the exception propagates. The next
     * run retries, which is safe because deleting a missing file is not an error.
     */
    public Result purge(@NonNull List<ConnectorMessageAttachment> attachments) {
        var outcome = deleteFiles(attachments);

        if (!outcome.deletedIdentifiers().isEmpty()) {
            attachmentRepository.deleteAllByIdentifiers(outcome.deletedIdentifiers());
        }

        return outcome.toResult();
    }

    private FilesOutcome deleteFiles(List<ConnectorMessageAttachment> attachments) {
        if (attachments.isEmpty()) {
            return FilesOutcome.EMPTY;
        }

        var deleted = new ArrayList<String>(attachments.size());
        var failed = 0;

        for (var attachment : attachments) {
            var identifier = attachment.identifier();
            try {
                fileStorageProvider.deleteByIdentifier(identifier);
                deleted.add(identifier);
            } catch (RuntimeException e) {
                failed++;
                log.warn(
                    "Could not delete attachment [{}] from [{}] storage",
                    identifier, fileStorageProvider.getStorage(), e
                );
            }
        }

        return new FilesOutcome(deleted, failed);
    }

    private record FilesOutcome(List<String> deletedIdentifiers, int failed) {
        static final FilesOutcome EMPTY = new FilesOutcome(List.of(), 0);

        Result toResult() {
            return new Result(deletedIdentifiers.size(), failed);
        }
    }

    /**
     * Outcome of a deletion: how many files were deleted and how many failed.
     */
    public record Result(int deleted, int failed) {
        public static final Result NONE = new Result(0, 0);

        public Result plus(Result other) {
            return new Result(deleted + other.deleted, failed + other.failed);
        }
    }
}
