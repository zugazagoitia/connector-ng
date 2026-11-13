/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration;

import eu.ecodex.connector.domain.model.message.content.ConnectorBusinessDocumentAESType;
import eu.ecodex.connector.domain.model.security.KeystoreType;
import eu.ecodex.connector.infrastructure.config.property.ConnectorSignatureValidationProperties;
import eu.ecodex.connector.infrastructure.config.property.businessdocument.AuthenticationValidationProperties;
import eu.ecodex.connector.infrastructure.config.property.businessdocument.BusinessDocumentSignatureProperties;
import eu.ecodex.connector.infrastructure.config.property.businessdocument.ConnectorBusinessDocumentProperties;
import eu.ecodex.connector.infrastructure.config.property.common.KeystoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for the connector business document configuration.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
public class ConnectorBusinessDocumentPropertiesDto {
    private String country;
    private String serviceProvider;
    private ConnectorBusinessDocumentAESType defaultAdvancedSystemType;
    private AuthenticationValidationDto authenticationValidation;
    private SignatureDto signature;

    public static ConnectorBusinessDocumentPropertiesDto from(
        ConnectorBusinessDocumentProperties p) {
        return ConnectorBusinessDocumentPropertiesDto
            .builder()
            .country(p.getCountry())
            .serviceProvider(p.getServiceProvider())
            .defaultAdvancedSystemType(p.getDefaultAdvancedSystemType())
            .authenticationValidation(AuthenticationValidationDto.from(
                p.getAuthenticationValidation()))
            .signature(SignatureDto.from(p.getSignature()))
            .build();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticationValidationDto {
        private String identityProvider;

        public static AuthenticationValidationDto from(AuthenticationValidationProperties p) {
            return p == null ? null : new AuthenticationValidationDto(p.getIdentityProvider());
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignatureDto {
        private SignatureValidationDto validation;

        public static SignatureDto from(BusinessDocumentSignatureProperties p) {
            return p == null ? null
                : new SignatureDto(SignatureValidationDto.from(p.getValidation()));
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignatureValidationDto {
        private String constraintsXml;
        private boolean ocspEnabled;
        private boolean crlEnabled;
        private boolean aiaEnabled;
        private boolean truststoreEnabled;
        private boolean ignoreStoreEnabled;
        private String trustedListSource;
        private KeystoreDto truststore;
        private KeystoreDto ignoreStore;

        public static SignatureValidationDto from(ConnectorSignatureValidationProperties p) {
            if (p == null) {
                return null;
            }

            var constraintsXml = p.getConstraintsXml() == null
                ? null
                : p.getConstraintsXml().getDescription();

            return SignatureValidationDto
                .builder()
                .constraintsXml(constraintsXml)
                .ocspEnabled(p.isOcspEnabled())
                .crlEnabled(p.isCrlEnabled())
                .aiaEnabled(p.isAiaEnabled())
                .truststoreEnabled(p.isTruststoreEnabled())
                .ignoreStoreEnabled(p.isIgnoreStoreEnabled())
                .trustedListSource(p.getTrustedListSource())
                .truststore(KeystoreDto.from(p.getTruststore()))
                .ignoreStore(KeystoreDto.from(p.getIgnoreStore()))
                .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeystoreDto {
        private String path;
        private KeystoreType type;

        public static KeystoreDto from(KeystoreProperties p) {
            return p == null ? null : new KeystoreDto(p.getPath(), p.getType());
        }
    }
}
