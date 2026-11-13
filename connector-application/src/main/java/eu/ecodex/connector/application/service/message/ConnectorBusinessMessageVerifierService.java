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

import eu.ecodex.connector.application.exception.NotFoundException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeActionNotFoundException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeServiceNotFoundException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeVerificationException;
import eu.ecodex.connector.application.port.api.message.ConnectorBusinessMessageVerifier;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeActionRepository;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModePartyRepository;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeServiceRepository;
import eu.ecodex.connector.domain.model.ProcessingModeVerificationMode;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ConnectorBusinessMessageVerifier} service.
 */
@Slf4j
@Service
public class ConnectorBusinessMessageVerifierService implements ConnectorBusinessMessageVerifier {
    private final ConnectorProcessingModePartyRepository partyRepository;
    private final ConnectorProcessingModeServiceRepository serviceRepository;
    private final ConnectorProcessingModeActionRepository actionRepository;

    /**
     * Creates a new {@code ConnectorMessageVerifierService}.
     *
     * @param partyRepository   repository used to resolve and validate message parties
     * @param serviceRepository repository used to resolve and validate message services
     * @param actionRepository  repository used to resolve and validate message actions
     */
    public ConnectorBusinessMessageVerifierService(
        ConnectorProcessingModePartyRepository partyRepository,
        ConnectorProcessingModeServiceRepository serviceRepository,
        ConnectorProcessingModeActionRepository actionRepository) {
        this.partyRepository = partyRepository;
        this.serviceRepository = serviceRepository;
        this.actionRepository = actionRepository;
    }

    @Override
    public void verify(
        @NonNull ConnectorMessage message,
        @NonNull ProcessingModeVerificationMode verificationMode) {
        log.debug(
            "Verifying message [{}] with verification mode [{}]",
            message.identifier(), verificationMode
        );

        switch (verificationMode) {
            case STRICT -> this.processStrictVerification(message);
            case RELAXED -> this.processRelaxedVerification(message);
            default -> this.processCreateVerification(message);
        }
    }

    private void processRelaxedVerification(ConnectorMessage message) {
        this.processServiceAndActionVerification(message);
    }

    private void processStrictVerification(ConnectorMessage message) {
        this.processServiceAndActionVerification(message);

        if (message.businessDomainIdentifier() == null) {
            throw new IllegalStateException("Business domain identifier is null");
        }

        var toParty = message.as4Properties().toParty();

        if (toParty.identifierType().isBlank()) {
            log.warn(
                "Message with identifier [{}] verification mode is RELAXED. "
                    + "Assuming ToParty IdentifierType [{}] as empty!",
                message.identifier(), toParty.identifierType()
            );
        }

        // TODO improve the verification
        var foundToParty = this.partyRepository.findByIdentifierAndRoleTypeAndBusinessDomain(
            toParty.identifier(), toParty.roleType(), message.businessDomainIdentifier()
        );

        if (foundToParty == null) {
            throw new ConnectorProcessingModeVerificationException(
                String.format(
                    "Message toParty [%s] is not configured on the connector! "
                        + "Check the P-Mode linked to business domain with uuid [%s]",
                    toParty, message.businessDomainIdentifier()
                )
            );
        }

        var fromParty = message.as4Properties().fromParty();

        if (fromParty.identifierType().isBlank()) {
            log.warn(
                "Message with identifier [{}] verification mode is RELAXED. "
                    + "Assuming FromParty IdentifierType [{}] as empty!",
                message.identifier(), fromParty.identifierType()
            );
        }

        var foundFromParty = this.partyRepository.findByIdentifierAndRoleTypeAndBusinessDomain(
            fromParty.identifier(), fromParty.roleType(), message.businessDomainIdentifier()
        );

        if (foundFromParty == null) {
            throw new ConnectorProcessingModeVerificationException(
                String.format(
                    "Message fromParty [%s] is not configured on the connector! "
                        + "Check the P-Mode linked to business domain with uuid [%s]",
                    fromParty, message.businessDomainIdentifier()
                )
            );
        }
    }

    private void processCreateVerification(ConnectorMessage message) {
        var warning = String.format(
            "Message with identifier [%s] verification failed because P-Mode CREATE "
                + "verification mode is not supported!",
            message.identifier()
        );
        log.warn(warning);
        throw new ConnectorProcessingModeVerificationException(warning);
    }

    private void processServiceAndActionVerification(ConnectorMessage message) {
        log.debug("Verifying service and action for message [{}]", message.identifier());
        var businessDomainIdentifier = message.businessDomainIdentifier();

        if (businessDomainIdentifier == null) {
            throw new IllegalStateException("Business domain identifier is null");
        }

        var as4Properties = message.as4Properties();

        try {
            var serviceName = as4Properties.service().name();
            var service = this.serviceRepository.findByNameAndBusinessDomain(
                serviceName,
                businessDomainIdentifier
            );

            if (service == null) {
                log.warn(
                    "Service with name [{}] and business domain [{}] not found",
                    serviceName, businessDomainIdentifier
                );
                throw new ConnectorProcessingModeServiceNotFoundException(
                    "Service [{" + serviceName + "}] not found"
                );
            }

            var action = this.actionRepository.findByNameAndBusinessDomain(
                as4Properties.action().name(), message.businessDomainIdentifier()
            );

            var actionName = as4Properties.action().name();

            if (action == null) {
                log.warn(
                    "Action with name [{}] and business domain [{}] not found",
                    actionName, businessDomainIdentifier
                );

                throw new ConnectorProcessingModeActionNotFoundException(
                    "action [{" + actionName + "}] not found"
                );
            }
        } catch (NotFoundException e) {
            log.error("Message with identifier [{}] verification failed", message.identifier());

            throw new ConnectorProcessingModeVerificationException(
                "Message verification failed", e
            );
        }
    }
}
