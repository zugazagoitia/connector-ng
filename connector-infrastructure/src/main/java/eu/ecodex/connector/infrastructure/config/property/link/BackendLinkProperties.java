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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for a backend component.
 *
 * <p>A backend configuration defines how the application communicates with other systems through a
 * specific link configuration and the associated link partners.
 */
@Getter
@Setter
public class BackendLinkProperties {
    @Valid
    @NotNull
    @NestedConfigurationProperty
    private LinkConfigProperties linkConfig;

    @Valid
    private List<LinkPartnerProperties> linkPartners = new ArrayList<>();
}
