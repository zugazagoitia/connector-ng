/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.certificate;

import eu.ecodex.connector.infrastructure.config.property.common.KeystoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for the certificate verifier.
 */
@Getter
@Setter
public class ConnectorCertificateVerifierProperties {
    /**
     * should ocsp be queried?.
     */
    private boolean ocspEnabled = true;
    /**
     * should a crl be queried?.
     */
    private boolean crlEnabled = true;
    /**
     * should AIA be used?.
     */
    private boolean aiaEnabled = true;

    private boolean truststoreEnabled = true;

    private boolean ignoreStoreEnabled = false;

    private String trustedListSource;

    @Valid
    @NestedConfigurationProperty
    private KeystoreProperties truststore;

    @Valid
    @NestedConfigurationProperty
    private KeystoreProperties ignoreStore;

    @AssertTrue(message = "ignore-store must be configured when ignore-store-enabled is true")
    public boolean isIgnoreStoreConfigured() {
        return !ignoreStoreEnabled || ignoreStore != null;
    }
}
