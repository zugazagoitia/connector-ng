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

import eu.ecodex.connector.domain.model.link.ConnectorLinkMode;
import eu.ecodex.connector.domain.model.security.KeystoreType;
import eu.ecodex.connector.infrastructure.config.property.common.KeystoreProperties;
import eu.ecodex.connector.infrastructure.config.property.common.PrivateKeyProperties;
import eu.ecodex.connector.infrastructure.config.property.link.BackendLinkProperties;
import eu.ecodex.connector.infrastructure.config.property.link.ConnectorLinkProperties;
import eu.ecodex.connector.infrastructure.config.property.link.LinkConfigDetailProperties;
import eu.ecodex.connector.infrastructure.config.property.link.LinkConfigProperties;
import eu.ecodex.connector.infrastructure.config.property.link.LinkEndpointProperties;
import eu.ecodex.connector.infrastructure.config.property.link.LinkPartnerDetailProperties;
import eu.ecodex.connector.infrastructure.config.property.link.LinkPartnerProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for defining the backend link configuration properties used in the connector.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
public class ConnectorLinkPropertiesDto {

    private List<BackendLinkDto> backend;

    public static ConnectorLinkPropertiesDto from(ConnectorLinkProperties properties) {
        return ConnectorLinkPropertiesDto
            .builder()
            .backend(properties.getBackend().stream().map(BackendLinkDto::from).toList())
            .build();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackendLinkDto {
        private LinkConfigDto linkConfig;
        private List<LinkPartnerDto> linkPartners;

        public static BackendLinkDto from(BackendLinkProperties p) {
            return BackendLinkDto.builder()
                                 .linkConfig(LinkConfigDto.from(p.getLinkConfig()))
                                 .linkPartners(p.getLinkPartners()
                                                .stream()
                                                .map(LinkPartnerDto::from)
                                                .toList())
                                 .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkConfigDto {
        private String name;
        private LinkConfigDetailDto properties;

        public static LinkConfigDto from(LinkConfigProperties p) {
            return p == null
                ? null
                : LinkConfigDto.builder()
                               .name(p.getName())
                               .properties(LinkConfigDetailDto.from(p.getProperties()))
                               .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkConfigDetailDto {
        private LinkEndpointDto endpoint;
        private String publishAddress;
        private boolean loggingEnabled;
        private String wsPolicy;

        public static LinkConfigDetailDto from(LinkConfigDetailProperties p) {
            if (p == null) {
                return null;
            }

            var wsPolicy = p.getWsPolicy() == null
                ? null
                : p.getWsPolicy().getDescription();

            return LinkConfigDetailDto.builder()
                                      .endpoint(LinkEndpointDto.from(p.getEndpoint()))
                                      .publishAddress(p.getPublishAddress())
                                      .loggingEnabled(p.isLoggingEnabled())
                                      .wsPolicy(wsPolicy)
                                      .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkEndpointDto {
        private KeystoreDto keystore;
        private PrivateKeyDto privateKey;
        private KeystoreDto truststore;
        private String encryptAlias;

        public static LinkEndpointDto from(LinkEndpointProperties p) {
            return p == null ? null : LinkEndpointDto
                                      .builder()
                                      .keystore(KeystoreDto.from(p.getKeystore()))
                                      .privateKey(PrivateKeyDto.from(p.getPrivateKey()))
                                      .truststore(KeystoreDto.from(p.getTruststore()))
                                      .encryptAlias(p.getEncryptAlias())
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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrivateKeyDto {
        private String alias;

        public static PrivateKeyDto from(PrivateKeyProperties p) {
            return p == null ? null : new PrivateKeyDto(p.getAlias());
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkPartnerDto {
        private String name;
        private String description;
        private boolean enabled;
        private ConnectorLinkMode senderMode;
        private LinkPartnerDetailDto properties;

        public static LinkPartnerDto from(LinkPartnerProperties p) {
            return LinkPartnerDto.builder()
                                 .name(p.getName())
                                 .description(p.getDescription())
                                 .enabled(p.isEnabled())
                                 .senderMode(p.getSenderMode())
                                 .properties(LinkPartnerDetailDto.from(p.getProperties()))
                                 .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkPartnerDetailDto {
        private String encryptionAlias;
        private String certificateDn;
        private String pushAddress;

        public static LinkPartnerDetailDto from(LinkPartnerDetailProperties p) {
            if (p == null) {
                return null;
            }

            var pushAddress = p.getPushAddress() == null ? null : p.getPushAddress().toString();
            return LinkPartnerDetailDto.builder()
                                       .encryptionAlias(p.getEncryptionAlias())
                                       .certificateDn(p.getCertificateDn())
                                       .pushAddress(pushAddress)
                                       .build();
        }
    }
}
