/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message.inbound;

import eu.ecodex.connector.application.port.api.businessdomain.ConnectorBusinessDomainVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorBusinessMessageVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorMessageIdGenerator;
import eu.ecodex.connector.application.port.api.message.inbound.ConnectorInboundBusinessMessageCommand;
import eu.ecodex.connector.application.port.api.message.inbound.ConnectorInboundBusinessMessageReceiver;
import eu.ecodex.connector.application.port.api.pmode.ConnectorProcessingModeVerifier;
import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.application.port.spi.ConnectorMessageProcessingConfigurationProvider;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Service responsible for receiving and processing inbound business messages.
 */
@Slf4j
@Service
public class ConnectorInboundBusinessMessageReceiverService implements
    ConnectorInboundBusinessMessageReceiver {
    private final ConnectorBusinessDomainVerifier businessDomainVerifierService;
    private final ConnectorProcessingModeVerifier processingModeVerifierService;
    private final ConnectorMessageProcessingConfigurationProvider configurationProvider;
    private final ConnectorBusinessMessageVerifier messageVerifierService;
    private final ConnectorMessageIdGenerator messageIdGeneratorService;
    private final ConnectorMessageEventPublisher<ConnectorBusinessMessage> stagingEventPublisher;

    /**
     * Constructs an instance of {@code ConnectorInboundBusinessMessageReceiverService}.
     *
     * @param businessDomainVerifierService the service responsible for verifying the business
     *                                      domain.
     * @param processingModeVerifierService the service responsible for verifying the processing
     *                                      mode.
     * @param configurationProvider         the provider of message processing configurations.
     * @param messageVerifierService        the service used for verifying business messages.
     * @param messageIdGeneratorService     the service for generating unique message identifiers.
     * @param stagingEventPublisher         the publisher responsible for sending inbound business
     *                                      messages to the JMS pipeline.
     */
    public ConnectorInboundBusinessMessageReceiverService(
        ConnectorBusinessDomainVerifier businessDomainVerifierService,
        ConnectorProcessingModeVerifier processingModeVerifierService,
        ConnectorMessageProcessingConfigurationProvider configurationProvider,
        ConnectorBusinessMessageVerifier messageVerifierService,
        ConnectorMessageIdGenerator messageIdGeneratorService,
        @Qualifier("connectorJmsInboundMessageStagingPublisher")
        ConnectorMessageEventPublisher<ConnectorBusinessMessage> stagingEventPublisher) {
        this.businessDomainVerifierService = businessDomainVerifierService;
        this.processingModeVerifierService = processingModeVerifierService;
        this.configurationProvider = configurationProvider;
        this.messageVerifierService = messageVerifierService;
        this.messageIdGeneratorService = messageIdGeneratorService;
        this.stagingEventPublisher = stagingEventPublisher;
    }

    @Override
    public void execute(@NonNull ConnectorInboundBusinessMessageCommand command) {
        log.info("Received inbound business message {}", command);

        businessDomainVerifierService.execute(command.businessDomainIdentifier());
        processingModeVerifierService.execute(command.businessDomainIdentifier());

        var message = ConnectorBusinessMessage
            .builder()
            .identifier(this.messageIdGeneratorService.execute())
            .businessDomainIdentifier(command.businessDomainIdentifier())
            .gatewayName(command.gatewayName())
            .as4Properties(command.as4Properties())
            .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
            .status(ConnectorBusinessMessageStatus.CREATED)
            .businessContent(command.businessContent())
            .attachments(command.attachments())
            .transportedEvidences(command.transportedEvidences())
            .build();

        var configuration = this.configurationProvider.getConfiguration();

        this.messageVerifierService.verify(
            message,
            configuration.inboundMessageVerificationMode()
        );

        this.stagingEventPublisher.publish(message);
    }
}
