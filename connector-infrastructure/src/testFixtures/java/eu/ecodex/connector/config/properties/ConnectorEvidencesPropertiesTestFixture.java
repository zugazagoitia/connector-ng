/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.config.properties;

import eu.ecodex.connector.domain.model.security.KeystoreType;
import eu.ecodex.connector.infrastructure.config.property.common.KeystoreProperties;
import eu.ecodex.connector.infrastructure.config.property.common.PrivateKeyProperties;
import eu.ecodex.connector.infrastructure.config.property.evidence.As4IssuerPartyProperties;
import eu.ecodex.connector.infrastructure.config.property.evidence.ConnectorEvidencesProperties;
import eu.ecodex.connector.infrastructure.config.property.evidence.EvidencesIssuerProperties;
import eu.ecodex.connector.infrastructure.config.property.evidence.EvidencesSignatureProperties;
import eu.ecodex.connector.infrastructure.config.property.evidence.PostalAddressProperties;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class ConnectorEvidencesPropertiesTestFixture {
    public static ConnectorEvidencesProperties evidencesProperties() {
        var keystore = new KeystoreProperties();
        keystore.setPath(
            "classpath:/keystores/connector-keystore.jks");
        keystore.setPassword("12345");
        keystore.setType(KeystoreType.JKS);

        var privateKey = new PrivateKeyProperties();
        privateKey.setAlias("connector_blue");
        privateKey.setPassword("12345");

        var signature = new EvidencesSignatureProperties();
        signature.setKeystore(keystore);
        signature.setPrivateKey(privateKey);
        signature.setDigestAlgorithm(DigestAlgorithm.SHA256);

        var as4Party = new As4IssuerPartyProperties();
        as4Party.setName("Connector Local");
        as4Party.setEndpointAddress("mailto:gateway@connector.example");

        var postalAddress = new PostalAddressProperties();
        postalAddress.setStreet("Local Street");
        postalAddress.setLocality("Local City");
        postalAddress.setZipCode("0000");
        postalAddress.setCountry("EU");

        var issuer = new EvidencesIssuerProperties();
        issuer.setAs4Party(as4Party);
        issuer.setPostalAddress(postalAddress);

        var properties = new ConnectorEvidencesProperties();
        properties.setSignature(signature);
        properties.setIssuer(issuer);
        return properties;
    }
}
