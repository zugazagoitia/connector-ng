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
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for connector-to-connector test messages services.
 */
public record TestServiceProperties(
    @NotBlank
    @DefaultValue(ConnectorDefaults.DEFAULT_TEST_SERVICE_NAME)
    String name,

    @NotBlank
    @DefaultValue(ConnectorDefaults.DEFAULT_TEST_SERVICE_TYPE)
    String type
) {
}
