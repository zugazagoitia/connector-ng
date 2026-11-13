/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.c2ctest;

import eu.ecodex.connector.domain.ConnectorDefaults;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for connector-to-connector test messages.
 */
@Validated
@ConfigurationProperties(prefix = "connector.c2ctests")
public record Connector2ConnectorTestMessageProperties(
    @DefaultValue("true")
    boolean enabled,

    @NotBlank
    @DefaultValue(ConnectorDefaults.DEFAULT_TEST_ACTION_NAME)
    String action,

    @Valid
    @NotNull
    @NestedConfigurationProperty
    @DefaultValue
    TestServiceProperties service
) {
}
