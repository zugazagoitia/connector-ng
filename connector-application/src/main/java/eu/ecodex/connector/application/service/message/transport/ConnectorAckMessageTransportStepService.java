/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message.transport;

import eu.ecodex.connector.application.exception.message.ConnectorLinkPartnerNotOwnerException;
import eu.ecodex.connector.application.exception.message.ConnectorMessageTransportStepNotFoundException;
import eu.ecodex.connector.application.port.api.transport.ConnectorAckMessageTransportStep;
import eu.ecodex.connector.application.port.api.transport.command.UpdateMessageTransportCommand;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageErrorRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageEvidenceRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorEvidenceMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.ConnectorMessageError;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link ConnectorAckMessageTransportStep}.
 */
@Slf4j
@Service
@Transactional
public class ConnectorAckMessageTransportStepService implements ConnectorAckMessageTransportStep {
    // TODO exclude FAILED status from this set when retrying is implemented
    private static final Set<ConnectorMessageTransportStatus> TERMINAL_STATUSES = Set.of(
        ConnectorMessageTransportStatus.FAILED,
        ConnectorMessageTransportStatus.DELIVERED
    );

    private final ConnectorMessageTransportStepRepository transportStepRepository;
    private final ConnectorMessageRepository messageRepository;
    private final ConnectorMessageEvidenceRepository evidenceRepository;
    private final ConnectorMessageErrorRepository messageErrorRepository;

    /**
     * Constructor for the ConnectorUpdateMessageTransportStepService class. Initializes the service
     * with the required repositories.
     *
     * @param transportStepRepository the repository used for managing message transport steps
     * @param messageRepository       the repository used for managing connector messages
     * @param evidenceRepository      the repository used for managing connector message evidences
     * @param messageErrorRepository  the repository used for managing connector message errors
     */
    public ConnectorAckMessageTransportStepService(
        ConnectorMessageTransportStepRepository transportStepRepository,
        ConnectorMessageRepository messageRepository,
        ConnectorMessageEvidenceRepository evidenceRepository,
        ConnectorMessageErrorRepository messageErrorRepository) {
        this.transportStepRepository = transportStepRepository;
        this.messageRepository = messageRepository;
        this.evidenceRepository = evidenceRepository;
        this.messageErrorRepository = messageErrorRepository;
    }

    @Override
    public void execute(
        @NonNull String stepOrMessageOrRemoteSystemIdentifier,
        @NonNull UpdateMessageTransportCommand command) {
        validateRequest(stepOrMessageOrRemoteSystemIdentifier, command);

        var existingStep = findStepOrThrow(stepOrMessageOrRemoteSystemIdentifier);
        assertOwnedBy(
            existingStep,
            command.linkPartnerName(),
            stepOrMessageOrRemoteSystemIdentifier
        );

        if (TERMINAL_STATUSES.contains(existingStep.status())) {
            log.info(
                "Message [{}] is already in terminal transport status [{}]",
                stepOrMessageOrRemoteSystemIdentifier, existingStep.status()
            );
            return;
        }

        updateTransportStep(existingStep, command);
        handlePostUpdate(existingStep, command);
    }

    private void validateRequest(String identifier, UpdateMessageTransportCommand command) {
        if (!StringUtils.hasText(identifier)) {
            throw new IllegalArgumentException(
                "Transported message or remote system identifier must not be empty");
        }

        if (command.status() == ConnectorMessageTransportStatus.FAILED
            && (command.errors() == null || command.errors().isEmpty())) {
            throw new IllegalArgumentException("Errors must be provided for failed transport");
        }

        if (command.status() == ConnectorMessageTransportStatus.READY_FOR_PULL) {
            throw new IllegalArgumentException(
                "Status READY_FOR_PULL cannot be applied to an existing transport step");
        }
    }

    private ConnectorMessageTransportStep findStepOrThrow(String identifier) {
        var step = transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(identifier);

        if (step == null) {
            throw new ConnectorMessageTransportStepNotFoundException(
                "No transport step found for identifier [%s]".formatted(identifier));
        }

        return step;
    }

    private void assertOwnedBy(
        ConnectorMessageTransportStep step,
        String linkPartnerName,
        String identifier) {
        if (!step.linkPartnerName().equalsIgnoreCase(linkPartnerName)) {
            throw new ConnectorLinkPartnerNotOwnerException(
                "The transport step with message/remote ID [%s] doesn't belong to link partner [%s]"
                    .formatted(identifier, linkPartnerName));
        }
    }

    private void updateTransportStep(
        ConnectorMessageTransportStep existingStep,
        UpdateMessageTransportCommand command) {

        transportStepRepository.update(
            existingStep.identifier(),
            command.status(),
            existingStep.numberOfAttempts()
        );
    }

    private void handlePostUpdate(
        ConnectorMessageTransportStep existingStep,
        UpdateMessageTransportCommand command) {

        var transportedMessage = existingStep.transportedMessage();

        switch (command.status()) {
            case PULLED -> handlePulled(transportedMessage);
            case DELIVERED ->
                handleDelivered(transportedMessage, command.remoteMessageIdentifier());
            case FAILED -> handleFailed(existingStep, command.errors());
            default -> log.debug("No action required for transport status [{}]", command.status());
        }
    }

    private void handlePulled(ConnectorMessage transportedMessage) {
        if (transportedMessage instanceof ConnectorBusinessMessage businessMessage) {
            messageRepository.updateStatus(
                businessMessage.identifier(),
                ConnectorBusinessMessageStatus.PULLED
            );
        }
    }

    private void handleDelivered(
        ConnectorMessage transportedMessage,
        String remoteMessageIdentifier) {
        switch (transportedMessage) {
            case ConnectorBusinessMessage businessMessage ->
                markBusinessMessageAsDelivered(businessMessage, remoteMessageIdentifier);
            case ConnectorEvidenceMessage evidenceMessage ->
                markEvidenceMessageAsDelivered(evidenceMessage);
        }
    }

    private void handleFailed(
        ConnectorMessageTransportStep step,
        List<ConnectorMessageError> errors) {
        if (step.transportedMessage() instanceof ConnectorBusinessMessage businessMessage) {
            registerErrors(businessMessage, errors);
        } else {
            // TODO for evidence messages, the errors should be registered to the step
            log.warn(
                "Errors of failed evidence transport step [{}] are not persisted yet",
                step.identifier()
            );
        }
    }

    private void markBusinessMessageAsDelivered(
        ConnectorBusinessMessage message,
        String remoteMessageIdentifier) {
        var identifier = message.identifier();

        if (messageRepository.findByIdentifier(identifier) == null) {
            log.warn("Message [{}] not found", identifier);
            return;
        }

        if (message.direction() == ConnectorMessageDirection.GATEWAY_TO_BACKEND) {
            messageRepository.updateBackendIdentifier(identifier, remoteMessageIdentifier);
            messageRepository.updateStatus(identifier, ConnectorBusinessMessageStatus.DELIVERED);
            log.info("Message [{}] delivered to backend link partner", identifier);
        } else {
            // TODO see if ebms ID should be updated
            log.info("Message [{}] delivered to gateway link partner", identifier);
        }

        messageRepository.setDeliveredToLinkPartnerAt(identifier);
    }

    private void markEvidenceMessageAsDelivered(ConnectorEvidenceMessage message) {
        var evidenceUuid = message.transportedEvidences()
                                  .stream()
                                  .findFirst()
                                  .map(ConnectorMessageEvidence::uuid)
                                  .orElseThrow(() -> new IllegalStateException(
                                      "Evidence message [%s] contains no transported evidence"
                                          .formatted(message.identifier())));

        evidenceRepository.setDeliveredToLinkPartnerAt(evidenceUuid);
    }

    private void registerErrors(
        ConnectorBusinessMessage transportedMessage,
        List<ConnectorMessageError> errors) {
        var messageIdentifier = transportedMessage.identifier();

        messageErrorRepository.save(messageIdentifier, errors);
        messageRepository.updateStatus(messageIdentifier, ConnectorBusinessMessageStatus.FAILED);
    }
}

