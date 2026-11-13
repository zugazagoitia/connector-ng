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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for a timestamp server.
 */
@Getter
@Setter
public class DssTimestampServerProperties {
    /**
     * TSA endpoint URL.
     */
    @NotBlank
    private String url;

    /**
     * TSA policy OID.
     */
    @Pattern(regexp = "[0-2](\\.\\d+)+", message = "must be a valid OID, e.g. 1.2.3.4")
    private String policyOid;
}
