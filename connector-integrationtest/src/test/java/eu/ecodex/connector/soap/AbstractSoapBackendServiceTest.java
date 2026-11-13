/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.soap;

import eu.ecodex.connector.AbstractIntegrationTest;
import eu.ecodex.connector.domain.model.security.KeystoreType;
import eu.ecodex.connector.domain.transition.DomibusConnectorBackendWebService;
import eu.ecodex.connector.infrastructure.adapter.out.soap.ConnectorMerlinPropertiesFactory;
import eu.ecodex.connector.infrastructure.adapter.out.soap.ConnectorWsPolicyLoader;
import eu.ecodex.connector.infrastructure.adapter.out.soap.KeystorePasswordCallback;
import eu.ecodex.connector.infrastructure.config.property.common.KeystoreProperties;
import eu.ecodex.connector.infrastructure.config.property.common.PrivateKeyProperties;
import eu.ecodex.connector.infrastructure.config.property.link.LinkConfigDetailProperties;
import eu.ecodex.connector.infrastructure.config.property.link.LinkEndpointProperties;
import java.util.HashMap;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.rt.security.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

public abstract class AbstractSoapBackendServiceTest extends AbstractIntegrationTest {
    @Autowired
    private ConnectorMerlinPropertiesFactory merlinPropertiesFactory;

    protected DomibusConnectorBackendWebService createClient(int port) {
        var address = "http://localhost:" + port + "/services/backend";

        var factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(DomibusConnectorBackendWebService.class);
        factory.setAddress(address);
        factory.setWsdlURL("classpath:wsdl/v1/DomibusConnectorBackendWebService.wsdl");

        var linkProperties = createLinkProperties();

        if (linkProperties.isLoggingEnabled()) {
            factory.getFeatures().add(new LoggingFeature());
        }

        var linkEndpointProperties = linkProperties.getEndpoint();

        var encryptionProperties = merlinPropertiesFactory.createEncryptionProperties(
            linkEndpointProperties);
        var sigingProperties = merlinPropertiesFactory.createSigningProperties(
            linkEndpointProperties);

        var privateKey = linkEndpointProperties.getPrivateKey();

        var properties = new HashMap<String, Object>();

        properties.put(
            SecurityConstants.ENCRYPT_USERNAME,
            linkEndpointProperties.getEncryptAlias()
        );
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
        properties.put(SecurityConstants.SIGNATURE_USERNAME, privateKey.getAlias());
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES, sigingProperties);
        properties.put(
            SecurityConstants.CALLBACK_HANDLER,
            new KeystorePasswordCallback(privateKey.getPassword())
        );

        properties.put("mtom-enabled", true);
        properties.put("security.store.bytes.in.attachment", true);
        properties.put("security.enable.streaming", true);

        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.setProperties(properties);

        var policyLoader = new ConnectorWsPolicyLoader(
            linkProperties.getWsPolicy()
        );
        factory.getFeatures().add(policyLoader.loadPolicyFeature());

        return (DomibusConnectorBackendWebService) factory.create();
    }

    private LinkConfigDetailProperties createLinkProperties() {
        var keystore = new KeystoreProperties();
        keystore.setPath("classpath:/keystores/backend-keystore.jks");
        keystore.setPassword("12345");
        keystore.setType(KeystoreType.JKS);

        var privateKey = new PrivateKeyProperties();
        privateKey.setAlias("alice");
        privateKey.setPassword("12345");

        var truststore = new KeystoreProperties();
        truststore.setPath("classpath:/keystores/backend-keystore.jks");
        truststore.setPassword("12345");
        truststore.setType(KeystoreType.JKS);

        var endpoint = new LinkEndpointProperties();
        endpoint.setKeystore(keystore);
        endpoint.setPrivateKey(privateKey);
        endpoint.setTruststore(truststore);
        endpoint.setEncryptAlias("connector_blue");

        var properties = new LinkConfigDetailProperties();
        properties.setEndpoint(endpoint);
        properties.setPublishAddress("/backend");
        properties.setLoggingEnabled(false);
        properties.setWsPolicy(new ClassPathResource("policy/backend.policy.xml"));

        return properties;
    }
}
