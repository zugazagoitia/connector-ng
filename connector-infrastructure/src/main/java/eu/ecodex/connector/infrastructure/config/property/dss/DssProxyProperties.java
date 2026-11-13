/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.dss;

import eu.europa.esig.dss.service.http.proxy.ProxyProperties;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * DSS proxy configuration.
 */
@Getter
@Setter
public class DssProxyProperties {
    @Valid
    @NestedConfigurationProperty
    ProxyProperties http;

    @Valid
    @NestedConfigurationProperty
    ProxyProperties https;
}
