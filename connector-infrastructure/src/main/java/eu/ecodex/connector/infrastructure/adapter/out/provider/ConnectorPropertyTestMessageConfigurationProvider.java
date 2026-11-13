/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.provider;

import eu.ecodex.connector.application.port.spi.ConnectorTestMessageConfigurationProvider;
import eu.ecodex.connector.domain.model.c2ctest.ConnectorTestMessageConfiguration;
import eu.ecodex.connector.domain.model.c2ctest.ConnectorTestMessageService;
import eu.ecodex.connector.infrastructure.config.property.c2ctest.Connector2ConnectorTestMessageProperties;
import org.springframework.stereotype.Component;

/**
 * Provides the connector test message configuration from application properties.
 */
@Component
public class ConnectorPropertyTestMessageConfigurationProvider implements
    ConnectorTestMessageConfigurationProvider {
    private final Connector2ConnectorTestMessageProperties testMessageProperties;

    public ConnectorPropertyTestMessageConfigurationProvider(
        Connector2ConnectorTestMessageProperties testMessageProperties) {
        this.testMessageProperties = testMessageProperties;
    }

    @Override
    public ConnectorTestMessageConfiguration getConfig() {
        var service = ConnectorTestMessageService.builder()
                                                 .name(testMessageProperties.service().name())
                                                 .type(testMessageProperties.service().type())
                                                 .build();
        return ConnectorTestMessageConfiguration.builder()
                                                .enabled(testMessageProperties.enabled())
                                                .service(service)
                                                .action(testMessageProperties.action())
                                                .build();
    }
}
