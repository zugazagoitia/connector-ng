/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.soap;

import eu.ecodex.connector.application.port.spi.link.ConnectorLinkPartnerProvider;
import eu.ecodex.connector.domain.model.link.partner.ConnectorLinkPartnerName;
import eu.ecodex.connector.domain.transition.DomibusConnectorBackendDeliveryWebService;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.rt.security.SecurityConstants;
import org.springframework.stereotype.Component;

/**
 * Service client for interacting with the backend delivery web service via SOAP. This client
 * enables communication with specific backend link partners using dynamic configuration and
 * security context setup.
 *
 * <p>This class is responsible for creating instances of
 * {@link DomibusConnectorBackendDeliveryWebService} with the appropriate configuration for a given
 * backend name. It initializes the SOAP client with properties such as encryption, signing, and
 * policy configurations and loads the necessary WSDL and endpoints to establish the connection.
 */
@Slf4j
@Component
public class ConnectorBackendDeliveryServiceClient {
    private final ConnectorLinkPartnerProvider linkPartnerRepository;
    private final ConnectorLinkPartnerConfigFactory linkPartnerConfigFactory;
    private final ConnectorMerlinPropertiesFactory merlinPropertiesFactory;

    /**
     * Constructs a new instance of {@code BackendServiceClient}.
     *
     * @param linkPartnerRepository    The repository used for accessing and retrieving
     *                                 {@code ConnectorLinkPartner} instances.
     * @param linkPartnerConfigFactory The factory for creating and managing configurations of
     *                                 connector link partners.
     * @param merlinPropertiesFactory  The factory for providing Merlin-specific configuration
     *                                 properties.
     */
    public ConnectorBackendDeliveryServiceClient(
        ConnectorLinkPartnerProvider linkPartnerRepository,
        ConnectorLinkPartnerConfigFactory linkPartnerConfigFactory,
        ConnectorMerlinPropertiesFactory merlinPropertiesFactory) {
        this.linkPartnerRepository = linkPartnerRepository;
        this.linkPartnerConfigFactory = linkPartnerConfigFactory;
        this.merlinPropertiesFactory = merlinPropertiesFactory;
    }

    /**
     * Creates a new client for the Connector Backend Delivery Web Service. This method initializes
     * and configures a JaxWsProxyFactoryBean to communicate with the backend delivery web service
     * associated with the specified link partner.
     *
     * @param backendName The name of the backend link partner for which the client is to be
     *                    created. Must not be null or empty.
     *
     * @return An instance of {@code DomibusConnectorBackendDeliveryWebService} configured to
     *     interact with the specified backend link partner.
     *
     * @throws IllegalStateException if the link partner or its configuration cannot be found.
     */
    public DomibusConnectorBackendDeliveryWebService createClient(String backendName) {
        log.debug("Creating backend client for {}", backendName);

        var linkPartnerName = new ConnectorLinkPartnerName(backendName);

        var linkPartner = this.linkPartnerRepository.findByName(linkPartnerName);

        if (linkPartner == null) {
            throw new IllegalStateException("Link partner " + linkPartnerName + " not found");
        }

        var linkPartnerConfig = this.linkPartnerConfigFactory.findByLinkPartnerName(
            linkPartnerName
        );

        if (linkPartnerConfig == null) {
            throw new IllegalStateException(
                "Link partner config " + linkPartnerName + " not found"
            );
        }

        var factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(DomibusConnectorBackendDeliveryWebService.class);
        factory.setAddress(linkPartner.pushAddress().toString());
        factory.setWsdlURL("classpath:wsdl/v1/DomibusConnectorBackendDeliveryWebService.wsdl");

        if (linkPartnerConfig.getProperties().isLoggingEnabled()) {
            factory.getFeatures().add(new LoggingFeature());
        }

        var policyLoader = new ConnectorWsPolicyLoader(
            linkPartnerConfig.getProperties().getWsPolicy()
        );
        factory.getFeatures().add(policyLoader.loadPolicyFeature());

        var linkEndpointProperties = linkPartnerConfig.getProperties().getEndpoint();

        var encryptionProperties = merlinPropertiesFactory.createEncryptionProperties(
            linkEndpointProperties);
        var sigingProperties = merlinPropertiesFactory.createSigningProperties(
            linkEndpointProperties);

        var jaxWsFactoryBeanProperties = new HashMap<String, Object>();
        var privateKey = linkEndpointProperties.getPrivateKey();
        jaxWsFactoryBeanProperties.put(
            SecurityConstants.ENCRYPT_USERNAME,
            linkPartner.encryptionAlias()
        );
        jaxWsFactoryBeanProperties.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
        jaxWsFactoryBeanProperties.put(SecurityConstants.SIGNATURE_USERNAME, privateKey.getAlias());
        jaxWsFactoryBeanProperties.put(SecurityConstants.SIGNATURE_PROPERTIES, sigingProperties);
        jaxWsFactoryBeanProperties.put(
            SecurityConstants.CALLBACK_HANDLER,
            new KeystorePasswordCallback(privateKey.getPassword())
        );
        jaxWsFactoryBeanProperties.put("mtom-enabled", true);

        factory.setProperties(jaxWsFactoryBeanProperties);

        return (DomibusConnectorBackendDeliveryWebService) factory.create();
    }
}
