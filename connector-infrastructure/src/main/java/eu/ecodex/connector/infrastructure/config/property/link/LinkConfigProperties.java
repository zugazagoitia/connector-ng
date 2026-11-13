/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.link;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Detailed configuration properties for a link partner.
 *
 * <p>These properties are typically used in backend configurations and define endpoint and
 * security-related settings required to communicate with the partner system.
 */
@Getter
@Setter
public class LinkConfigProperties {
    @NotBlank
    private String name;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    private LinkConfigDetailProperties properties;
}
