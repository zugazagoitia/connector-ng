/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property;

import eu.ecodex.connector.infrastructure.config.property.certificate.ConnectorCertificateVerifierProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for connector signature validation.
 */
@Getter
@Setter
public class ConnectorSignatureValidationProperties extends ConnectorCertificateVerifierProperties {
    /**
     * Location of the XML file defining the constraints (the validation policy) applied when
     * validating signatures. Any Spring {@link Resource} location is accepted, for example
     * {@code classpath:...}, {@code file:...} or {@code https:...}. Must not be null. Defaults to
     * the policy bundled in the application, {@code 102853/constraint.xml} on the classpath.
     */
    @NotNull
    private Resource constraintsXml = new ClassPathResource("102853/constraint.xml");

    /**
     * Validates that the constraints resource exists, so that a wrong location fails the startup
     * instead of the first signature validation.
     *
     * <p>A {@code null} resource is considered valid here, since {@link NotNull} on
     * {@link #constraintsXml} already reports it. Only the existence of the resource is checked,
     * not its content, so a malformed policy file is detected later, when it is parsed.
     *
     * @return {@code true} if the resource is {@code null} or exists, {@code false} otherwise
     */

    @AssertTrue(message = "constraints-xml resource does not exist")
    public boolean isConstraintsXmlPresent() {
        return constraintsXml == null || constraintsXml.exists();
    }
}
