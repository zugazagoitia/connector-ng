/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.container;

import eu.ecodex.connector.infrastructure.config.property.ConnectorSignatureProperties;
import eu.ecodex.connector.infrastructure.config.property.ConnectorSignatureValidationProperties;
import eu.ecodex.connector.infrastructure.config.property.common.KeystoreProperties;
import eu.ecodex.connector.infrastructure.config.property.common.PrivateKeyProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for the container signature.
 */
@Getter
@Setter
public class ContainerSignatureProperties extends ConnectorSignatureProperties {
    @Valid
    @NotNull
    @NestedConfigurationProperty
    private KeystoreProperties keystore;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    private PrivateKeyProperties privateKey;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    private ConnectorSignatureValidationProperties validation =
        new ConnectorSignatureValidationProperties();
}
