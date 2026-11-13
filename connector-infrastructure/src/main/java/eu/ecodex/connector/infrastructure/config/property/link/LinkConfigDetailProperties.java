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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Detailed configuration properties for a link configuration.
 *
 * <p>These properties define implementation-specific settings used by the link implementation.
 * Some settings apply only to gateway deployments, while others configure SOAP communication and
 * logging behavior.
 */
@Getter
@Setter
public class LinkConfigDetailProperties {
    @Valid
    @NotNull
    @NestedConfigurationProperty
    private LinkEndpointProperties endpoint;

    @Pattern(regexp = "/.*", message = "must start with /")
    private String publishAddress = "/backend";

    private boolean loggingEnabled = false;

    @NotNull
    private Resource wsPolicy = new ClassPathResource("policy/backend.policy.xml");
}
