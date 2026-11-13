/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;


/**
 * Configuration properties for the JWT authentication.
 */
@Getter
@Setter
@Validated
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "secret")
@ConfigurationProperties(prefix = "connector.auth.security.jwt")
public class ConnectorJwtProperties {
    @NotBlank
    private String secret;

    @NotNull
    private Duration expiration = Duration.ofHours(1);

    @NotBlank
    private String issuer;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    private ConnectorRefreshTokenProperties refreshToken;
}
