/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property;

import eu.ecodex.connector.domain.model.ProcessingModeVerificationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the connector message processing.
 */
@Validated
@ConfigurationProperties(prefix = "connector.message-processing")
public record ConnectorMessageProcessingProperties(
    @DefaultValue("true")
    boolean ebmsIdGeneratorEnabled,

    @NotBlank
    @DefaultValue("connector.ecodex.eu")
    String identifierSuffix,

    @NotBlank
    @DefaultValue("connector.ecodex.eu")
    String ebmsIdSuffix,

    @NotBlank
    @DefaultValue("connector.ecodex.eu")
    String transportIdSuffix,

    @NotNull
    @DefaultValue("STRICT")
    ProcessingModeVerificationMode outboundVerificationMode,

    @NotNull
    @DefaultValue("RELAXED")
    ProcessingModeVerificationMode inboundVerificationMode,

    @DefaultValue("true")
    boolean sendGeneratedEvidencesToBackend,

    @DefaultValue("true")
    boolean autoTriggerDeliveryEvidences
) {
}
