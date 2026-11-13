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

import eu.ecodex.connector.application.port.api.message.ConnectorCleanupUnresolvedMessages;
import eu.ecodex.connector.domain.model.filter.ConnectorMessagesListFilter;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.List;

/**
 * Repository (port) for persisting and querying {@link ConnectorBusinessMessage} objects.
 */
public interface ConnectorMessageRepository {
    /**
     * Persists the given business message.
     *
     * @param message the message to save, carrying its metadata and business content (must not be
     *                null)
     *
     * @return the saved {@link ConnectorBusinessMessage}, including any generated values such as
     *     timestamps or identifiers
     */
    ConnectorBusinessMessage save(@Nonnull ConnectorBusinessMessage message);

    /**
     * Updates the name of the gateway associated with a message.
     *
     * @param identifier the unique identifier of the message to update (must not be null)
     * @param name       the new gateway name (must not be null)
     *
     * @return the updated {@link ConnectorBusinessMessage}
     */
    ConnectorBusinessMessage updateGatewayName(@Nonnull String identifier, @Nonnull String name);

    /**
     * Updates the name of the backend associated with a message.
     *
     * @param identifier the unique identifier of the message to update (must not be null)
     * @param name       the new backend name (must not be null)
     *
     * @return the updated {@link ConnectorBusinessMessage}
     */
    ConnectorBusinessMessage updateBackendName(@Nonnull String identifier, @Nonnull String name);

    /**
     * Updates the ebMS message identifier of a message.
     *
     * @param identifier     the unique identifier of the message to update (must not be null)
     * @param ebmsIdentifier the new ebMS message identifier (must not be null)
     *
     * @return the updated {@link ConnectorBusinessMessage}
     */
    ConnectorBusinessMessage updateEbmsIdentifier(
        @Nonnull String identifier,
        @Nonnull String ebmsIdentifier);

    /**
     * Updates the backend message identifier of a message.
     *
     * @param identifier        the unique identifier of the message to update (must not be null)
     * @param backendIdentifier the new backend message identifier (must not be null)
     *
     * @return the updated {@link ConnectorBusinessMessage}
     */
    ConnectorBusinessMessage updateBackendIdentifier(
        @Nonnull String identifier,
        @Nonnull String backendIdentifier);

    /**
     * Updates the status of the message identified by the given identifier.
     *
     * @param identifier the unique identifier of the message to update (must not be null)
     * @param status     the new {@link ConnectorBusinessMessageStatus} of the message (must not be
     *                   null)
     *
     * @return the updated {@link ConnectorBusinessMessage}, reflecting the new status
     */
    ConnectorBusinessMessage updateStatus(
        @Nonnull String identifier,
        @Nonnull ConnectorBusinessMessageStatus status);

    /**
     * Retrieves a page of {@link ConnectorBusinessMessage} objects based on the pagination
     * parameters and the optional filter criteria carried by the given filter. All filters are
     * combined with a logical {@code AND}; a filter whose value is {@code null} or empty is
     * ignored.
     *
     * @param filter the pagination parameters and filter criteria (must not be null)
     *
     * @return a {@link ConnectorPageResult} containing the matching messages for the requested
     *     page, with pagination metadata such as the total number of elements and pages; empty if
     *     no message matches
     */
    ConnectorPageResult<ConnectorBusinessMessage> findAll(
        @Nonnull ConnectorMessagesListFilter filter);

    /**
     * Retrieves the unresolved business messages created before the given threshold.
     *
     * <p>A message is unresolved when it has not reached a final state, that is, it is still
     * waiting for a transport or processing outcome (for example an acknowledgement or an
     * evidence). It is used by {@link ConnectorCleanupUnresolvedMessages}.
     *
     * @param threshold the instant before which unresolved messages are returned; messages created
     *                  at or after this instant are excluded (must not be null)
     *
     * @return the unresolved {@link ConnectorBusinessMessage} objects created before the threshold;
     *     never {@code null}, and empty if there are none
     */
    List<ConnectorBusinessMessage> findAllUnresolved(Instant threshold);

    /**
     * Retrieves all messages whose identifier is in the given list.
     *
     * @param identifiers the identifiers of the messages to retrieve (must not be null); if the
     *                    list is empty, no result is returned
     *
     * @return the matching {@link ConnectorBusinessMessage} objects; empty if none match
     */
    List<ConnectorBusinessMessage> findAllByIdentifier(@Nonnull List<String> identifiers);

    /**
     * Finds a message by its unique identifier.
     *
     * @param identifier the unique identifier of the message (must not be null or blank)
     *
     * @return the matching {@link ConnectorBusinessMessage}, or {@code null} if none exists
     */
    ConnectorBusinessMessage findByIdentifier(@Nonnull String identifier);

    /**
     * Finds a message by its ebMS message identifier and direction.
     *
     * @param ebmsMessageIdentifier the ebMS message identifier (must not be null or blank)
     * @param direction             the direction of the message (must not be null)
     *
     * @return the matching {@link ConnectorBusinessMessage}, or {@code null} if none exists
     */
    ConnectorBusinessMessage findByEbmsMessageIdentifierAndDirection(
        @Nonnull String ebmsMessageIdentifier, @Nonnull ConnectorMessageDirection direction);

    /**
     * Finds the business message referenced by an evidence trigger.
     *
     * @param referenceToMessageIdentifier the identifier of the referenced message, as carried by
     *                                     the trigger
     * @param triggerDirection             the direction of the trigger message
     *
     * @return the referenced {@link ConnectorBusinessMessage}, or {@code null} if none exists
     */
    ConnectorBusinessMessage findReferencedBusinessMessage(
        String referenceToMessageIdentifier, ConnectorMessageDirection triggerDirection);

    /**
     * Retrieves the messages belonging to the given conversation.
     *
     * @param conversationIdentifier the unique identifier of the conversation (must not be null or
     *                               blank)
     *
     * @return the {@link ConnectorBusinessMessage} objects of the conversation; empty if none
     */
    List<ConnectorBusinessMessage> findByConversationIdentifier(
        @Nonnull String conversationIdentifier);

    /**
     * Marks a message as rejected by updating its status.
     *
     * @param identifier the unique identifier of the message to mark (must not be null)
     *
     * @return the updated {@link ConnectorBusinessMessage}, reflecting the rejected status
     */
    ConnectorBusinessMessage setAsRejected(@Nonnull String identifier);

    /**
     * Marks a message as confirmed by updating its status.
     *
     * @param identifier the unique identifier of the message to mark (must not be null)
     *
     * @return the updated {@link ConnectorBusinessMessage}, reflecting the confirmed status
     */
    ConnectorBusinessMessage setAsConfirmed(@Nonnull String identifier);

    /**
     * Marks a message as deleted by recording the deletion instant (soft delete).
     *
     * <p>A message is deleted once its attachments and its business content (the business content
     * and business document files) have been deleted. The method records this state on the message;
     * the deletion of the files and of the attachment and business content rows is done beforehand
     * The message itself is not removed from the repository: it is flagged as deleted.
     *
     * @param identifier the unique identifier of the message to mark as deleted (must not be null)
     */

    void setAsDeleted(@Nonnull String identifier);

    /**
     * Records that a message has been delivered to the link partner, by setting its delivery
     * timestamp.
     *
     * @param identifier the unique identifier of the message (must not be null)
     *
     * @return the updated {@link ConnectorBusinessMessage}
     */
    ConnectorBusinessMessage setDeliveredToLinkPartnerAt(@Nonnull String identifier);

    /**
     * Permanently deletes the message identified by the given identifier.
     *
     * <p>Unlike {@link #setAsDeleted}, which only flags the message as deleted, this physically
     * removes the message row. Nothing happens if no message matches. The records that depend on
     * the message (business content, AS4 properties, attachments, evidences, errors and transport
     * steps) must be removed first, or by a cascade, otherwise the deletion fails with a foreign
     * key violation.
     *
     * @param identifier the unique identifier of the message to delete (must not be null)
     */
    void deleteByIdentifier(@Nonnull String identifier);
}
