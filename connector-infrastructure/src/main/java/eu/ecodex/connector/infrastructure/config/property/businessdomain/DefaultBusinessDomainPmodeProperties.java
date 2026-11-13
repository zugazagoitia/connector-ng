/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.businessdomain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Default business domain P-Mode properties.
 */
@Getter
@Setter
public class DefaultBusinessDomainPmodeProperties {
    @Valid
    @NotBlank
    private String file;

    @Valid
    @NotBlank
    private String truststore;

    @Valid
    @NotBlank
    private String truststorePassword;
}
