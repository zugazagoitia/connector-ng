/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message;

import eu.ecodex.connector.application.exception.evidence.ConnectorEvidenceException;
import eu.ecodex.connector.application.exception.evidence.ConnectorEvidenceNotRelevantException;
import eu.ecodex.connector.application.port.api.message.ConnectorMessageEvidenceVerifier;
import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.domain.model.ConnectorErrorCode;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.ConnectorMessageResolvedEvent;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorEvidenceType;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ConnectorMessageEvidenceVerifier}.
 */
@Slf4j
@Component
@Transactional
public class ConnectorMessageEvidenceVerifierService implements ConnectorMessageEvidenceVerifier {
    private static final EnumSet<ConnectorEvidenceType> NEGATIVE_EVIDENCE_TYPES = EnumSet.of(
        ConnectorEvidenceType.SUBMISSION_REJECTION,
        ConnectorEvidenceType.NON_DELIVERY,
        ConnectorEvidenceType.NON_RETRIEVAL,
        ConnectorEvidenceType.RELAY_REMMD_REJECTION,
        ConnectorEvidenceType.RELAY_REMMD_FAILURE
    );
    private static final EnumSet<ConnectorEvidenceType> POSITIVE_EVIDENCE_TYPES = EnumSet.of(
        ConnectorEvidenceType.DELIVERY,
        ConnectorEvidenceType.RETRIEVAL
    );

    private final ConnectorMessageRepository messageRepository;
    private final ConnectorMessageEventPublisher<ConnectorMessageResolvedEvent>
        attachmentCleanupPublisher;

    /**
     * Creates the verifier.
     *
     * @param messageRepository          the repository used to read and update the messages the
     *                                   evidences refer to (must not be null)
     * @param attachmentCleanupPublisher the publisher used to send a
     *                                   {@link ConnectorMessageResolvedEvent} when a message
     *                                   becomes resolved, which triggers the cleanup of its
     *                                   attachments (must not be null)
     */
    public ConnectorMessageEvidenceVerifierService(
        ConnectorMessageRepository messageRepository,
        @Qualifier("connectorJmsResolvedMessagePublisher")
        ConnectorMessageEventPublisher<ConnectorMessageResolvedEvent>
            attachmentCleanupPublisher) {
        this.messageRepository = messageRepository;
        this.attachmentCleanupPublisher =
            attachmentCleanupPublisher;
    }

    @Override
    public void verify(
        @NonNull ConnectorEvidenceType evidenceType,
        @NonNull ConnectorBusinessMessage message) {
        var messageIdentifier = message.identifier();

        log.debug("Processing message [{}] with evidence [{}]", messageIdentifier, evidenceType);

        var transportedEvidences = message.transportedEvidences();

        if (transportedEvidences == null || transportedEvidences.isEmpty()) {
            throw new ConnectorEvidenceException(
                "Message [{" + messageIdentifier + "}] has no evidences!"
            );
        }

        int highestPriority = getHighestReceivedEvidencePriority(transportedEvidences);

        if (evidenceType.getPriority() < highestPriority) {
            log.info(
                "Evidence [{}] will not influence the rejected or confirmed state of message "
                    + "[{}] because it has lower priority than the already received evidences. "
                    + "error_code=[{}]",
                evidenceType,
                messageIdentifier,
                ConnectorErrorCode.EVIDENCE_IGNORED_DUE_HIGHER_PRIORITY.getCode()
            );

            throw new ConnectorEvidenceNotRelevantException(
                ConnectorErrorCode.EVIDENCE_IGNORED_DUE_HIGHER_PRIORITY
            );
        }

        if (evidenceType == ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE
            && message.direction() == ConnectorMessageDirection.BACKEND_TO_GATEWAY) {
            messageRepository.updateStatus(
                messageIdentifier,
                ConnectorBusinessMessageStatus.RECEIVED_BY_OPPOSITE_CONNECTOR
            );
        }

        if (NEGATIVE_EVIDENCE_TYPES.contains(evidenceType)) {
            log.warn(
                "Message [{}] has been rejected by evidence [{}]",
                messageIdentifier,
                evidenceType
            );

            this.messageRepository.setAsRejected(messageIdentifier);
            this.attachmentCleanupPublisher.publish(
                new ConnectorMessageResolvedEvent(messageIdentifier)
            );

            return;
        }

        if (POSITIVE_EVIDENCE_TYPES.contains(evidenceType)) {
            var currentMessage = this.messageRepository.findByIdentifier(message.identifier());
            if (currentMessage != null && currentMessage.isRejected()) {
                log.warn(
                    "Message [{}] has already been rejected by an negative evidence! "
                        + "The positive evidence of type [{}] will be ignored!",
                    messageIdentifier, evidenceType
                );

                throw new ConnectorEvidenceNotRelevantException(
                    ConnectorErrorCode.EVIDENCE_IGNORED_MESSAGE_ALREADY_REJECTED
                );
            }

            this.messageRepository.setAsConfirmed(message.identifier());
            this.attachmentCleanupPublisher.publish(
                new ConnectorMessageResolvedEvent(messageIdentifier)
            );

            log.info(
                "Message [{}] has been confirmed by evidence [{}]",
                messageIdentifier,
                evidenceType
            );
        }
    }

    private int getHighestReceivedEvidencePriority(List<ConnectorMessageEvidence> evidences) {
        return evidences.stream()
                        .map(evidence -> evidence.type().getPriority())
                        .max(Comparator.naturalOrder())
                        .orElse(0);
    }
}
