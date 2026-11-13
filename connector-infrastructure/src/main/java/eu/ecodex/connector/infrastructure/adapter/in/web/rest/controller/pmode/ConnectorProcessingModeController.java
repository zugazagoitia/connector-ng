/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.pmode;

import eu.ecodex.connector.application.port.api.pmode.ConnectorListProcessingModeActions;
import eu.ecodex.connector.application.port.api.pmode.ConnectorListProcessingModeParties;
import eu.ecodex.connector.application.port.api.pmode.ConnectorListProcessingModeServices;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeAction;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeService;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.pmode.ConnectorProcessingModeActionDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.pmode.ConnectorProcessingModeServiceDto;
import eu.ecodex.connector.infrastructure.config.property.c2ctest.Connector2ConnectorTestMessageProperties;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
 * Defines the REST controller for managing processing modes within the connector system.
 */
@RestController
public class ConnectorProcessingModeController implements ConnectorProcessingModeApi {
    private final ConnectorListProcessingModeServices listProcessingModeServices;
    private final ConnectorListProcessingModeActions listProcessingModeActions;
    private final ConnectorListProcessingModeParties listProcessingModeParties;
    private final Connector2ConnectorTestMessageProperties testMessageProperties;

    /**
     * Constructs an instance of the {@code ConnectorProcessingModeController}, which is responsible
     * for managing processing modes within the connector system.
     *
     * @param listProcessingModeServices the service handling operations related to listing
     *                                   {@link ConnectorProcessingModeService} entities in the
     *                                   processing mode.
     * @param listProcessingModeActions  the service handling operations related to listing
     *                                   {@link ConnectorProcessingModeAction} entities in the
     *                                   processing mode.
     * @param listProcessingModeParties  the service handling operations related to listing
     *                                   {@link ConnectorProcessingModeParty} entities in the
     *                                   processing mode.
     * @param testMessageProperties      the property for connector2Connector test message
     */
    public ConnectorProcessingModeController(
        ConnectorListProcessingModeServices listProcessingModeServices,
        ConnectorListProcessingModeActions listProcessingModeActions,
        ConnectorListProcessingModeParties listProcessingModeParties,
        Connector2ConnectorTestMessageProperties testMessageProperties) {
        this.listProcessingModeServices = listProcessingModeServices;
        this.listProcessingModeActions = listProcessingModeActions;
        this.listProcessingModeParties = listProcessingModeParties;
        this.testMessageProperties = testMessageProperties;
    }

    @Override
    public List<ConnectorProcessingModeServiceDto> listProcessingModeServices(
        String businessDomainIdentifier) {
        return listProcessingModeServices
            .execute(businessDomainIdentifier)
            .stream()
            .map(service -> ConnectorProcessingModeServiceDto.from(
                service,
                testMessageProperties.service().name(),
                testMessageProperties.service().type()
            ))
            .toList();
    }

    @Override
    public List<ConnectorProcessingModeActionDto> listProcessingModeActions(
        String businessDomainIdentifier) {
        return this.listProcessingModeActions
            .execute(businessDomainIdentifier)
            .stream()
            .map(action -> ConnectorProcessingModeActionDto.from(
                action, testMessageProperties.action()
            ))
            .toList();
    }

    @Override
    public List<ConnectorProcessingModeParty> listProcessingModeParties(
        String businessDomainIdentifier) {
        return listProcessingModeParties.execute(businessDomainIdentifier);
    }
}
