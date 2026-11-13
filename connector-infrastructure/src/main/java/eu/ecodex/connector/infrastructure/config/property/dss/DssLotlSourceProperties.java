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

import eu.ecodex.connector.infrastructure.config.property.common.KeystoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for the LOTL source.
 */
@Getter
@Setter
public class DssLotlSourceProperties {
    /**
     * URL of the LOTL XML, e.g. <a href="https://ec.europa.eu/tools/lotl/eu-lotl.xml">...</a>.
     */
    @NotBlank
    private String lotlUrl = "https://ec.europa.eu/tools/lotl/eu-lotl.xml";

    /**
     * OJ announcement URI for signing certificate validation.
     */
    @NotBlank
    private String signingCertificatesAnnouncementUri = "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=uriserv:OJ.C_.2019.276.01.0001.01.ENG";

    /**
     * Whether pivot LOTL support is enabled.
     */
    private boolean pivotSupport = true;

    @Valid
    @NestedConfigurationProperty
    private KeystoreProperties signingCerts;
}
