/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.routing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the connector message routing.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "connector.routing")
public class ConnectorMessageRoutingProperties {
    private boolean enabled;

    private String defaultBackendName;

    @Valid
    @NotNull
    private List<BackendRuleProperties> backendRules = new ArrayList<>();

    @AssertTrue(message = "default-backend-name is required when routing is enabled")
    public boolean isDefaultBackendNameConfigured() {
        return !enabled || (defaultBackendName != null && !defaultBackendName.isBlank());
    }
}
