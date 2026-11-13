/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.businessdocument;

import eu.ecodex.connector.domain.model.message.content.ConnectorBusinessDocumentAESType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the business document.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "connector.business-document")
public class ConnectorBusinessDocumentProperties {
    @NotBlank
    String country;

    @NotBlank
    String serviceProvider;

    @NotNull
    ConnectorBusinessDocumentAESType defaultAdvancedSystemType;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    AuthenticationValidationProperties authenticationValidation;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    BusinessDocumentSignatureProperties signature = new BusinessDocumentSignatureProperties();

    @NotEmpty
    Set<ConnectorBusinessDocumentAESType> allowedAdvancedSystemTypes =
        EnumSet.allOf(ConnectorBusinessDocumentAESType.class);


    @AssertTrue(
        message = "default-advanced-system-type must be one of allowed-advanced-system-types"
    )
    public boolean isDefaultAdvancedSystemTypeAllowed() {
        return allowedAdvancedSystemTypes.contains(defaultAdvancedSystemType);
    }
}
