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
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the default business domain in the connector. Provides the ability
 * to configure the identifier, description, enabled status, and source of the business domain
 * through externalized configuration.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "connector.business-domains")
public class ConnectorBusinessDomainProperties {
    @Valid
    private List<DefaultBusinessDomainProperties> defaults;
}
