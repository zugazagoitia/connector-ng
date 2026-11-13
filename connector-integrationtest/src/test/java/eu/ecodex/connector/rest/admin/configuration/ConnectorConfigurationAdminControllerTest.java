/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.rest.admin.configuration;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import eu.ecodex.connector.AbstractIntegrationTest;
import eu.ecodex.connector.domain.model.message.content.ConnectorBusinessDocumentAESType;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.ConnectorBusinessDocumentPropertiesDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.ConnectorBusinessDomainPropertiesDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.ConnectorEvidencesPropertiesDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.ConnectorLinkPropertiesDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.ConnectorMessageProcessingPropertiesDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.ConnectorMessageRoutingPropertiesDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.ConnectorQueuePropertiesDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.container.ConnectorContainerPropertiesDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

public class ConnectorConfigurationAdminControllerTest extends AbstractIntegrationTest {
    private static final String BASE_URL = "/api/v1/admin/configurations";

    @Autowired
    private RestTestClient apiClient;

    @Test
    @Sql({"classpath:sql/user.sql"})
    void should_list_business_domains_configurations_successfully() {
        apiClient.get()
                 .uri(BASE_URL + "/business-domains")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<ConnectorBusinessDomainPropertiesDto>() {
                 })
                 .value(result -> {
                     assert result != null;
                     var defaults = result.defaults();
                     assertThat(defaults).isNotEmpty();
                 });
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void should_list_container_configurations_successfully() {
        apiClient.get()
                 .uri(BASE_URL + "/container")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<ConnectorContainerPropertiesDto>() {
                 })
                 .value(result -> {
                     assert result != null;
                     var signature = result.signature();
                     assertThat(signature).isNotNull();
                     assertThat(signature.privateKey()).isNotNull();
                     assertThat(signature.keystore()).isNotNull();
                 });
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void should_list_queues_configuration_successfully() {
        apiClient.get()
                 .uri(BASE_URL + "/queues")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<ConnectorQueuePropertiesDto>() {
                 })
                 .value(result -> {
                     assertThat(result).isNotNull();
                     assert result != null;
                     assertThat(result.outboundEvidenceTriggerQueue()).isNotNull();
                 });
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void should_list_message_processing_configuration_successfully() {
        apiClient.get()
                 .uri(BASE_URL + "/message-processing")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(
                     new ParameterizedTypeReference<ConnectorMessageProcessingPropertiesDto>() {
                     })
                 .value(result -> {
                     assertThat(result).isNotNull();
                     assert result != null;
                     assertThat(result.identifierSuffix()).isNotNull();
                     assertThat(result.ebmsIdGeneratorEnabled()).isTrue();
                 });
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void should_list_evidence_configuration_successfully() {
        apiClient.get()
                 .uri(BASE_URL + "/evidence")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<ConnectorEvidencesPropertiesDto>() {
                 })
                 .value(result -> {
                     assertThat(result).isNotNull();
                     assert result != null;
                     assertThat(result.signature()).isNotNull();
                     assertThat(result.issuer()).isNotNull();
                 });
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void should_list_business_document_configuration_successfully() {
        apiClient.get()
                 .uri(BASE_URL + "/business-document")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<ConnectorBusinessDocumentPropertiesDto>() {
                 })
                 .value(result -> {
                     assert result != null;
                     assertThat(result.getCountry()).isNotNull();
                     assertThat(result.getCountry()).isEqualTo("BL");
                     assertThat(result.getServiceProvider()).isEqualTo("TestProvider");
                     assertThat(result.getSignature()).isNotNull();
                     assertThat(result.getDefaultAdvancedSystemType()).isEqualTo(
                         ConnectorBusinessDocumentAESType.SIGNATURE_BASED);
                     assertThat(result.getAuthenticationValidation()).isNotNull();
                     assertThat(result.getSignature()).isNotNull();
                 });
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void should_list_message_routing_configuration_successfully() {
        apiClient.get()
                 .uri(BASE_URL + "/routing")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<ConnectorMessageRoutingPropertiesDto>() {
                 })
                 .value(result -> {
                     assertThat(result).isNotNull();
                     assert result != null;
                     assertThat(result.enabled()).isTrue();
                     assertThat(result.defaultBackendName()).isEqualTo("backend_alice");
                     assertThat(result.backendRules()).isNotEmpty();
                 });
    }

    @Test
    @Sql({"classpath:sql/user.sql"})
    void should_list_backend_link_partners_configuration_successfully() {
        apiClient.get()
                 .uri(BASE_URL + "/backend-link-partners")
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateDefaultAdminToken())
                 .exchange()
                 .expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<ConnectorLinkPropertiesDto>() {
                 })
                 .value(result -> {
                     assertThat(result).isNotNull();
                     assert result != null;
                     assertThat(result.getBackend()).isNotEmpty();
                     assertThat(result.getBackend().getFirst().getLinkConfig()).isNotNull();
                 });
    }
}
