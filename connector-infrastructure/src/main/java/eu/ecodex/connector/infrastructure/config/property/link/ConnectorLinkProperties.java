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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Root configuration properties for the connector link module.
 *
 * <p>This class aggregates configuration for both the gateway and backend components involved in
 * the connector link setup.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "connector.link")
public class ConnectorLinkProperties {
    @Valid
    private List<BackendLinkProperties> backend = new ArrayList<>();
}
