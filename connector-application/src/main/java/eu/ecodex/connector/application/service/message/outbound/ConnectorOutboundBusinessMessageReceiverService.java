/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message.outbound;

import eu.ecodex.connector.application.port.api.attachment.ConnectorAttachmentVerifier;
import eu.ecodex.connector.application.port.api.businessdomain.ConnectorBusinessDomainVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorBusinessMessageVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorMessageIdGenerator;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundBusinessMessageCommand;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundBusinessMessageReceiver;
import eu.ecodex.connector.application.port.api.pmode.ConnectorProcessingModeVerifier;
import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.application.port.spi.ConnectorMessageProcessingConfigurationProvider;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.processing.ConnectorMessageProcessingConfiguration;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ConnectorOutboundBusinessMessageReceiver}.
 */
@Slf4j
@Service
@Transactional
public class ConnectorOutboundBusinessMessageReceiverService
    implements ConnectorOutboundBusinessMessageReceiver {
    private final ConnectorMessageProcessingConfigurationProvider configurationProvider;
    private final ConnectorBusinessMessageVerifier messageVerifierService;
    private final ConnectorMessageEventPublisher<ConnectorBusinessMessage> stagingEventPublisher;
    private final ConnectorMessageIdGenerator messageIdGeneratorService;
    private final ConnectorBusinessDomainVerifier businessDomainVerifierService;
    private final ConnectorProcessingModeVerifier processingModeVerifierService;
    private final ConnectorAttachmentVerifier attachmentVerifierService;

    /**
     * Constructs a new {@code ConnectorOutboundMessageReceiverService}.
     *
     * @param configurationProvider         provider of the current
     *                                      {@link ConnectorMessageProcessingConfiguration}
     * @param messageVerifierService        verifier used to validate outbound messages
     * @param stagingEventPublisher         event publisher used to stage messages for further
     *                                      processing; qualified as
     *                                      "connectorOutboundMessageStagingEventPublisher"
     * @param messageIdGeneratorService     generator used to assign unique identifiers to outbound
     *                                      messages param businessDomainVerifier verifier used to
     *                                      validate business domains of outbound messages
     * @param businessDomainVerifierService verifier used to validate business domains of outbound
     *                                      messages
     * @param processingModeVerifierService verifier used to validate processing modes of outbound
     *                                      messages
     * @param attachmentVerifierService     verifier used to validate attachments identifier of
     *                                      outbound message
     */
    public ConnectorOutboundBusinessMessageReceiverService(
        ConnectorMessageProcessingConfigurationProvider configurationProvider,
        ConnectorBusinessMessageVerifier messageVerifierService,
        @Qualifier("connectorJmsOutboundMessageStagingPublisher")
        ConnectorMessageEventPublisher<ConnectorBusinessMessage> stagingEventPublisher,
        ConnectorMessageIdGenerator messageIdGeneratorService,
        ConnectorBusinessDomainVerifier businessDomainVerifierService,
        ConnectorProcessingModeVerifier processingModeVerifierService,
        ConnectorAttachmentVerifier attachmentVerifierService) {
        this.configurationProvider = configurationProvider;
        this.messageVerifierService = messageVerifierService;
        this.stagingEventPublisher = stagingEventPublisher;
        this.messageIdGeneratorService = messageIdGeneratorService;
        this.businessDomainVerifierService = businessDomainVerifierService;
        this.processingModeVerifierService = processingModeVerifierService;
        this.attachmentVerifierService = attachmentVerifierService;
    }

    @Override
    public ConnectorBusinessMessage execute(
        @NonNull ConnectorOutboundBusinessMessageCommand command) {
        log.info("Received outbound business message {}", command);
        businessDomainVerifierService.execute(command.businessDomainIdentifier());
        processingModeVerifierService.execute(command.businessDomainIdentifier());
        attachmentVerifierService.execute(command.attachments());

        var message = ConnectorBusinessMessage
            .builder()
            .identifier(this.messageIdGeneratorService.execute())
            .businessDomainIdentifier(command.businessDomainIdentifier())
            .backendMessageIdentifier(command.backendMessageIdentifier())
            .backendName(command.backendName())
            .as4Properties(command.as4Properties())
            .direction(command.direction())
            .status(ConnectorBusinessMessageStatus.CREATED)
            .businessContent(command.businessContent())
            .attachments(command.attachments())
            .build();

        var configuration = this.configurationProvider.getConfiguration();
        this.messageVerifierService.verify(
            message,
            configuration.outboundMessageVerificationMode()
        );
        stagingEventPublisher.publish(message);

        return message;
    }
}
