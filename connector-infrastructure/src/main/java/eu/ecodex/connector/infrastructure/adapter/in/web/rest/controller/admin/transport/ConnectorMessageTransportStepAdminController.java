/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.transport;

import eu.ecodex.connector.application.port.api.message.ConnectorRetrieveMessage;
import eu.ecodex.connector.application.port.api.transport.ConnectorListTransportSteps;
import eu.ecodex.connector.application.port.api.transport.ConnectorRetrieveTransportStepByTransportId;
import eu.ecodex.connector.domain.model.filter.ConnectorDateRangeFilter;
import eu.ecodex.connector.domain.model.filter.ConnectorTransportStepsListFilter;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorEvidenceMessage;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import eu.ecodex.connector.domain.model.paging.ConnectorPageRequest;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import eu.ecodex.connector.domain.model.paging.SortDirection;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.message.ConnectorMessageDetailDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.transport.ConnectorMessageTransportStepDto;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
 * Defines the REST controller for managing transport steps within the connector system.
 */
@RestController
public class ConnectorMessageTransportStepAdminController
    implements ConnectorMessageTransportStepAdminApi {
    private final ConnectorListTransportSteps listTransportSteps;
    private final ConnectorRetrieveTransportStepByTransportId
        retrieveTransportStepByTransportId;
    private final ConnectorRetrieveMessage retrieveMessage;

    /**
     * Creates the controller.
     *
     * @param listTransportSteps                 the use case listing transport steps (must not be
     *                                           null)
     * @param retrieveTransportStepByTransportId the use case retrieving a transport step by its
     *                                           transport identifier (must not be null)
     * @param retrieveMessage                    the use case retrieving a business message by its
     *                                           identifier (must not be null)
     */
    public ConnectorMessageTransportStepAdminController(
        ConnectorListTransportSteps listTransportSteps,
        ConnectorRetrieveTransportStepByTransportId retrieveTransportStepByTransportId,
        ConnectorRetrieveMessage retrieveMessage) {
        this.listTransportSteps = listTransportSteps;
        this.retrieveTransportStepByTransportId = retrieveTransportStepByTransportId;
        this.retrieveMessage = retrieveMessage;
    }

    @Override
    public ConnectorPageResult<ConnectorMessageTransportStepDto> listTransportSteps(
        int page,
        int size,
        String stepOrMessageOrRemoteSystemIdentifier,
        String linkPartnerName,
        List<ConnectorMessageTransportStatus> statuses,
        Instant from,
        Instant to) {
        var pageRequest = ConnectorPageRequest.of(page, size, "createdAt", SortDirection.DESC);
        var filter = ConnectorTransportStepsListFilter.of(
            pageRequest,
            stepOrMessageOrRemoteSystemIdentifier,
            linkPartnerName,
            statuses,
            ConnectorDateRangeFilter.of(from, to)
        );

        var transportSteps = listTransportSteps.execute(filter);

        return ConnectorPageResult.of(
            transportSteps.content()
                          .stream()
                          .map(ConnectorMessageTransportStepDto::from)
                          .toList(),
            transportSteps.size(),
            transportSteps.totalElements(),
            transportSteps.totalPages()
        );
    }

    @Override
    public ConnectorMessageDetailDto retrieveTransportedMessage(String identifier) {
        var step = retrieveTransportStepByTransportId.execute(identifier);
        var transportedMessage = step.transportedMessage();

        if (transportedMessage instanceof ConnectorBusinessMessage message) {
            return ConnectorMessageDetailDto.from(retrieveMessage.execute(message.identifier()));
        } else {
            return ConnectorMessageDetailDto.from(
                (ConnectorEvidenceMessage) transportedMessage,
                isDeliveredAt(step)
            );
        }
    }

    private Instant isDeliveredAt(ConnectorMessageTransportStep transportStep) {
        if (transportStep.status() == ConnectorMessageTransportStatus.DELIVERED
            || transportStep.status() == ConnectorMessageTransportStatus.PULLED) {
            return transportStep.updatedAt();
        }

        return null;
    }
}
