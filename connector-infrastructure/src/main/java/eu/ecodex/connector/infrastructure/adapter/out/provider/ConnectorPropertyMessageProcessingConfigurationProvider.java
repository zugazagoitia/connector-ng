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

import eu.ecodex.connector.application.port.spi.ConnectorMessageProcessingConfigurationProvider;
import eu.ecodex.connector.domain.model.message.processing.ConnectorMessageProcessingConfiguration;
import eu.ecodex.connector.infrastructure.config.property.ConnectorMessageProcessingProperties;
import org.springframework.stereotype.Component;

/**
 * Default {@link ConnectorMessageProcessingConfigurationProvider} that reads the message processing
 * configuration from application properties.
 *
 * <p>The configuration is bound once from the properties and exposed as is, so it does not change
 * at runtime unless the properties are rebound.
 */
@Component
public class ConnectorPropertyMessageProcessingConfigurationProvider implements
    ConnectorMessageProcessingConfigurationProvider {
    private final ConnectorMessageProcessingProperties messageProcessingProperties;

    public ConnectorPropertyMessageProcessingConfigurationProvider(
        ConnectorMessageProcessingProperties messageProcessingProperties) {
        this.messageProcessingProperties = messageProcessingProperties;
    }

    @Override
    public ConnectorMessageProcessingConfiguration getConfiguration() {
        return ConnectorMessageProcessingConfiguration
            .builder()
            .ebmsIdSuffix(messageProcessingProperties.ebmsIdSuffix())
            .identifierSuffix(messageProcessingProperties.identifierSuffix())
            .transportIdSuffix(messageProcessingProperties.transportIdSuffix())
            .sendGeneratedEvidencesToBackend(
                messageProcessingProperties.sendGeneratedEvidencesToBackend())
            .inboundMessageVerificationMode(
                messageProcessingProperties.inboundVerificationMode())
            .outboundMessageVerificationMode(
                messageProcessingProperties.outboundVerificationMode())
            .ebmsIdGeneratorEnabled(messageProcessingProperties.ebmsIdGeneratorEnabled())
            .autoTriggerDeliveryEvidences(
                messageProcessingProperties.autoTriggerDeliveryEvidences())
            .build();
    }
}
