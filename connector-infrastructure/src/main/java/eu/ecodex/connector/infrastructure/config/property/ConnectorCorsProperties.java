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

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for specifying Cross-Origin Resource Sharing (CORS) settings.
 */
@Validated
@ConfigurationProperties(prefix = "connector.cors")
public record ConnectorCorsProperties(
    @NotEmpty
    @DefaultValue("*")
    List<String> allowedOrigins,

    @NotEmpty
    @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    List<String> allowedMethods,

    @NotEmpty
    @DefaultValue("*")
    List<String> allowedHeaders,

    @DefaultValue("true")
    boolean allowCredentials,

    @DefaultValue("3600")
    long maxAge
) {
}
