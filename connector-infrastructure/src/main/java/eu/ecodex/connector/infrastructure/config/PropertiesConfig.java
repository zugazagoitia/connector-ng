/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that registers the connector's {@code @ConfigurationProperties} classes.
 *
 * <p>It scans the package {@code eu.ecodex.connector.infrastructure.config.property} (and its
 * subpackages) for classes annotated with {@link ConfigurationProperties} and registers them as
 * beans, so that they are bound from the application configuration and can be injected. Classes
 * that are not in this package are not detected.
 */
@Configuration
@ConfigurationPropertiesScan(
    basePackages = {
        "eu.ecodex.connector.infrastructure.config.property"
    }
)
public class PropertiesConfig {
}
