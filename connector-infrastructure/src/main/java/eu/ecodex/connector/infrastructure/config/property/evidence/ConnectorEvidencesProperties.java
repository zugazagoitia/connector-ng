/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.evidence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for REM evidence generation ({@code connector.evidences.*}).
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "connector.evidences")
public class ConnectorEvidencesProperties {
    @Valid
    @NotNull
    @NestedConfigurationProperty
    private EvidencesSignatureProperties signature = new EvidencesSignatureProperties();

    @Valid
    @NotNull
    @NestedConfigurationProperty
    private EvidencesIssuerProperties issuer = new EvidencesIssuerProperties();
}
