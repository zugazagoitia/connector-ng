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

import eu.ecodex.connector.application.port.api.transport.ConnectorSetMessagesTransportStepToPull;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ConnectorSetMessagesTransportStepToPull}.
 */
@Service
@Transactional
public class ConnectorSetMessagesTransportStepToPullService
    implements ConnectorSetMessagesTransportStepToPull {
    private final ConnectorMessageTransportStepRepository transportStepRepository;
    private final ConnectorMessageRepository messageRepository;

    public ConnectorSetMessagesTransportStepToPullService(
        ConnectorMessageTransportStepRepository transportStepRepository,
        ConnectorMessageRepository messageRepository) {
        this.transportStepRepository = transportStepRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public void execute(@NonNull String backendName) {
        var identifiers = this.transportStepRepository.findReadyToDownloadTransportStepsIds(
            backendName);
        this.transportStepRepository.updateStatus(
            identifiers,
            ConnectorMessageTransportStatus.PULLED
        );

        for (var identifier : identifiers) {
            var step = this.transportStepRepository.findByIdentifier(identifier);
            var transportedMessage = step.transportedMessage();
            if (transportedMessage instanceof ConnectorBusinessMessage businessMessage) {
                this.messageRepository.updateStatus(
                    businessMessage.identifier(),
                    ConnectorBusinessMessageStatus.PULLED
                );
            }
        }
    }
}
