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

import eu.ecodex.connector.application.port.api.transport.ConnectorSetMessagesTransportStepToPull;
import eu.ecodex.connector.domain.model.filter.ConnectorTransportStepsListFilter;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Repository (port) for persisting and querying {@link ConnectorMessageTransportStep} objects.
 */
public interface ConnectorMessageTransportStepRepository {
    /**
     * Persists the given transport step, inserting it or updating it if it already exists.
     *
     * @param transportStep the transport step to persist (must not be null)
     *
     * @return the persisted {@link ConnectorMessageTransportStep}, updated with any state assigned
     *     during persistence (for example timestamps)
     */
    ConnectorMessageTransportStep save(@Nonnull ConnectorMessageTransportStep transportStep);

    /**
     * Updates the transport step identified by the given identifier with the given state.
     *
     * @param identifier       the unique identifier of the transport step to update (must not be
     *                         null)
     * @param status           the new {@link ConnectorMessageTransportStatus} of the transport step
     *                         (must not be null)
     * @param numberOfAttempts the new number of transport attempts made so far
     */
    ConnectorMessageTransportStep update(
        @Nonnull String identifier,
        @Nonnull ConnectorMessageTransportStatus status,
        int numberOfAttempts);

    /**
     * Updates the status of several transport steps at once.
     *
     * @param identifiers the unique identifiers of the transport steps to update (must not be
     *                    null)
     * @param status      the new {@link ConnectorMessageTransportStatus} to assign to the
     *                    identified transport steps (must not be null)
     */
    void updateStatus(
        @Nonnull List<String> identifiers,
        @Nonnull ConnectorMessageTransportStatus status);

    /**
     * Retrieves a transport step by its own identifier, by the identifier of its message, or by a
     * remote system identifier (backend ID or ebMS ID).
     *
     * @param identifier the transport step identifier, the message identifier or the remote system
     *                   identifier (must not be null)
     *
     * @return the matching {@link ConnectorMessageTransportStep}, or {@code null} if none is found
     */
    ConnectorMessageTransportStep findByStepOrMessageOrRemoteSystemIdentifier(
        @Nonnull String identifier);

    /**
     * Retrieves a transport step by its own unique identifier.
     *
     * @param identifier the unique identifier of the transport step (must not be null)
     *
     * @return the matching {@link ConnectorMessageTransportStep}, or {@code null} if none is found
     */
    ConnectorMessageTransportStep findByIdentifier(@Nonnull String identifier);

    /**
     * Retrieves the identifiers of the transport steps of the given backend that are ready to be
     * downloaded.
     *
     * <p>A transport step is ready to download when its message has been made available to the
     * backend and has not yet been retrieved (see
     * {@link ConnectorSetMessagesTransportStepToPull}).
     *
     * @param backendName the name of the backend system (must not be null or empty)
     *
     * @return the identifiers of the transport steps ready to download; never {@code null}, and
     *     empty if there are none
     */
    List<String> findReadyToDownloadTransportStepsIds(@Nonnull String backendName);

    /**
     * Retrieves the pending transport steps of the given backend.
     *
     * <p>Pending transport steps are those whose message has not yet been submitted to the backend
     * system for processing.
     *
     * @param backendName the name of the backend system (must not be null or empty)
     *
     * @return the pending {@link ConnectorMessageTransportStep} objects of the backend; never
     *     {@code null}, and empty if there are none
     */
    List<ConnectorMessageTransportStep> findReadyToDownloadTransportSteps(
        @Nonnull String backendName);

    /**
     * Retrieves a page of transport steps based on the pagination parameters and the optional
     * filter criteria carried by the given filter. All filters are combined with a logical
     * {@code AND}; a filter whose value is {@code null} or empty is ignored.
     *
     * @param filter the pagination parameters and filter criteria (must not be null)
     *
     * @return a {@link ConnectorPageResult} containing the matching transport steps for the
     *     requested page; empty if none match
     */
    ConnectorPageResult<ConnectorMessageTransportStep> findAll(
        @Nonnull ConnectorTransportStepsListFilter filter);

    /**
     * Deletes all the transport steps of the message identified by the given identifier.
     *
     * <p>The transport steps are physically removed, not flagged as deleted. The message itself
     * is not affected, and nothing happens if the message has no transport step. This is part of
     * permanently deleting a message (see {@link ConnectorMessageRepository#deleteByIdentifier}).
     *
     * @param messageIdentifier the unique identifier of the transported business message whose
     *                          transport steps are deleted (must not be null)
     */
    void deleteByTransportedMessageIdentifier(@Nonnull String messageIdentifier);
}
