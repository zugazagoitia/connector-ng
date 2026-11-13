/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.port.api.attachment;

import eu.ecodex.connector.application.port.api.message.ConnectorMessageAttachmentLinker;
import jakarta.annotation.Nonnull;
import java.time.Instant;

/**
 * Use case for deleting orphaned attachments.
 *
 * <p>An attachment is orphaned when it was uploaded / created but never linked to a message (see
 * {@link ConnectorMessageAttachmentLinker}).
 */
public interface ConnectorCleanupOrphanedAttachments {

    /**
     * Deletes the orphaned attachments created before the given threshold.
     *
     * <p>The threshold protects attachments that were uploaded recently and may still be about to
     * be linked to a message: only orphaned attachments older than the threshold are removed.
     *
     * @param threshold the instant before which orphaned attachments are deleted; attachments
     *                  created at or after this instant are kept (must not be null)
     */
    void execute(@Nonnull Instant threshold);
}
