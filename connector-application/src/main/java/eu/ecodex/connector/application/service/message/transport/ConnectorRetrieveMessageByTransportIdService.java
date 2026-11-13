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

import eu.ecodex.connector.application.exception.NotFoundException;
import eu.ecodex.connector.application.exception.message.ConnectorLinkPartnerNotOwnerException;
import eu.ecodex.connector.application.exception.message.ConnectorMessageTransportStepException;
import eu.ecodex.connector.application.port.api.transport.ConnectorRetrieveMessageByTransportId;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ConnectorRetrieveMessageByTransportId}.
 */
@Slf4j
@Service
@Transactional
public class ConnectorRetrieveMessageByTransportIdService implements
    ConnectorRetrieveMessageByTransportId {
    private final ConnectorMessageTransportStepRepository transportStepRepository;

    public ConnectorRetrieveMessageByTransportIdService(
        ConnectorMessageTransportStepRepository transportStepRepository) {
        this.transportStepRepository = transportStepRepository;
    }

    @Override
    public ConnectorMessage execute(
        @NonNull String transportIdentifier,
        @Nonnull String linkPartnerName) {
        log.info("Retrieving message for transport identifier: [{}]", transportIdentifier);

        var transportStep = this.transportStepRepository.findByIdentifier(transportIdentifier);

        if (transportStep == null) {
            throw new NotFoundException(
                "No transport step found for identifier: " + transportIdentifier
            );
        }

        if (!transportStep.linkPartnerName().equalsIgnoreCase(linkPartnerName)) {
            throw new ConnectorLinkPartnerNotOwnerException(
                "The transport step [%s] does not belong to the link partner [%s]".formatted(
                    transportIdentifier,
                    linkPartnerName
                )
            );
        }

        if (transportStep.status() != ConnectorMessageTransportStatus.READY_FOR_PULL) {
            throw new ConnectorMessageTransportStepException(
                "The message with transport id ["
                    + transportIdentifier + "] is not in READY_FOR_PULL state!"
            );
        }

        return transportStep.transportedMessage();
    }
}
