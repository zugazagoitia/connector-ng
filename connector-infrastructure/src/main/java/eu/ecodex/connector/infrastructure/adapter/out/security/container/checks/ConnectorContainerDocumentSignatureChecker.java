/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.security.container.checks;

import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeRepository;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.security.ConnectorTruststore;
import eu.ecodex.connector.infrastructure.adapter.out.dss.ConnectorDssCertificateSourceLoader;
import eu.ecodex.connector.infrastructure.adapter.out.dss.ConnectorDssCertificateVerifier;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.container.ConnectorContainer;
import eu.ecodex.connector.infrastructure.config.property.container.ConnectorContainerProperties;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.jaxb.object.Message;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.signature.DefaultSignatureProcessExecutor;
import eu.europa.esig.dss.validation.reports.Reports;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ConnectorSignatureChecker is responsible for validating the signatures of DSS documents. It
 * ensures that the document signatures conform to the specified validation constraints and checks
 * for the presence of appropriate certificates in the connector's certificate source.
 */
@Slf4j
@Component("ConnectorContainerDocumentSignatureChecker")
public class ConnectorContainerDocumentSignatureChecker {
    private final ConnectorProcessingModeRepository processingModeRepository;
    private final ConnectorDssCertificateVerifier dssCertificateVerifier;
    private final ConnectorDssCertificateSourceLoader dssCertificateSourceLoader;
    private final ConnectorContainerProperties connectorContainerProperties;

    /**
     * Constructs an instance of {@code ConnectorContainerDocumentSignatureChecker}.
     *
     * @param processingModeRepository     the repository for managing processing modes associated
     *                                     with specific business domain identifiers; must not be
     *                                     null.
     * @param dssCertificateVerifier       the certificate verifier used for validating certificate
     *                                     trust and integrity; must not be null.
     * @param dssCertificateSourceLoader   the loader responsible for getting certificate sources
     *                                     used during the signature validation process; must not be
     *                                     null.
     * @param connectorContainerProperties the property configuration object containing relevant
     *                                     settings for container signature validation, must not be
     *                                     null.
     */
    public ConnectorContainerDocumentSignatureChecker(
        ConnectorProcessingModeRepository processingModeRepository,
        ConnectorDssCertificateVerifier dssCertificateVerifier,
        ConnectorDssCertificateSourceLoader dssCertificateSourceLoader,
        ConnectorContainerProperties connectorContainerProperties) {
        this.processingModeRepository = processingModeRepository;
        this.dssCertificateVerifier = dssCertificateVerifier;
        this.dssCertificateSourceLoader = dssCertificateSourceLoader;
        this.connectorContainerProperties = connectorContainerProperties;
    }

    /**
     * Validates the digital signatures of documents within a {@link ConnectorContainer}.
     *
     * <p>This method ensures that the provided container's XML, PDF, and ASiC documents are
     * verified against the specified business domain's truststore and associated certificate
     * sources and verifiers. It performs signature integrity and certificate trust validation for
     * the associated documents.
     *
     * @param container                the {@link ConnectorContainer} containing the documents to be
     *                                 validated; must not be null
     * @param businessDomainIdentifier the {@link ConnectorBusinessDomainIdentifier} used to
     *                                 retrieve truststore and configuration for signature
     *                                 validation; must not be null
     */
    public void check(
        @NonNull ConnectorContainer container,
        @NonNull ConnectorBusinessDomainIdentifier businessDomainIdentifier) {

        var truststore = resolveTruststore(businessDomainIdentifier);

        var signature = connectorContainerProperties.getSignature();
        var certificateSource =
            dssCertificateSourceLoader.createCertificateSource(signature.getKeystore());
        var certificateVerifier =
            dssCertificateVerifier.createCommonCertificateVerifier(
                signature.getValidation(), truststore);

        checkDocument(container.tokenXML(), certificateSource, certificateVerifier);
        checkDocument(container.tokenPDF(), certificateSource, certificateVerifier);
        checkDocument(container.asicDocument(), certificateSource, certificateVerifier);
    }

    private ConnectorTruststore resolveTruststore(
        ConnectorBusinessDomainIdentifier businessDomainIdentifier) {
        var processingMode =
            processingModeRepository.findByBusinessDomainIdentifier(businessDomainIdentifier);
        if (processingMode == null) {
            throw new IllegalStateException(
                "Processing mode for business domain identifier [%s] not found"
                    .formatted(businessDomainIdentifier));
        }

        var truststore = processingMode.truststore();
        if (truststore == null) {
            throw new IllegalStateException(
                "Processing mode for business domain identifier [%s] has no truststore configured"
                    .formatted(businessDomainIdentifier));
        }
        return truststore;
    }

    private void checkDocument(
        DSSDocument document,
        CertificateSource certificateSource,
        CertificateVerifier certificateVerifier) {

        var validator = SignedDocumentValidator.fromDocument(document);
        validator.setProcessExecutor(new DefaultSignatureProcessExecutor());
        validator.setCertificateVerifier(certificateVerifier);

        var reports = validate(validator);

        var signatures = validator.getSignatures();
        if (signatures.isEmpty()) {
            throw new IllegalStateException(
                "The validation report for [%s] does not contain any signature information"
                    .formatted(document.getName()));
        }

        var simpleReport = reports.getSimpleReport();
        log.debug(
            "DSS validation simple report for [{}]: [{}]", document.getName(), simpleReport);
        var diagnosticData = reports.getDiagnosticData();

        var errors = new ArrayList<String>();
        for (var signature : signatures) {
            checkSignature(
                signature, simpleReport, diagnosticData, document, certificateSource,
                errors
            );
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("; ", errors));
        }
    }

    private Reports validate(SignedDocumentValidator validator) {
        var policy = connectorContainerProperties.getSignature()
                                                         .getValidation()
                                                         .getConstraintsXml();

        if (policy == null) {
            return validator.validateDocument();
        }

        try (var policyStream = policy.getInputStream()) {
            return validator.validateDocument(policyStream);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Cannot open signature validation constraints [%s]"
                    .formatted(policy.getDescription()), e
            );
        }
    }

    private void checkSignature(
        AdvancedSignature signature,
        SimpleReport simpleReport,
        DiagnosticData diagnosticData,
        DSSDocument dssDocument,
        CertificateSource certificateSource,
        List<String> errors) {
        var signatureId = signature.getId();

        verifyCertificateSource(signature, signatureId, certificateSource, errors);

        var indication = simpleReport.getIndication(signatureId);
        if (Indication.PASSED.equals(indication) || Indication.TOTAL_PASSED.equals(indication)) {
            log.debug("Signature [{}] is valid", signatureId);
            return;
        }

        var qualificationErrors = simpleReport.getQualificationErrors(signatureId)
                                              .stream()
                                              .map(Message::getValue)
                                              .toList();

        if (qualificationErrors.isEmpty()) {
            var algorithm = SignatureAlgorithm.getAlgorithm(
                diagnosticData.getSignatureEncryptionAlgorithm(signatureId),
                diagnosticData.getSignatureDigestAlgorithm(signatureId)
            );
            errors.add(
                "Signature [%s] in [%s] has an invalid verification result for algorithm: %s"
                    .formatted(signatureId, dssDocument.getName(), algorithm));
        } else {
            errors.add(
                "Signature [%s]: %s"
                    .formatted(signatureId, String.join("/", qualificationErrors)));
        }
    }

    private void verifyCertificateSource(
        AdvancedSignature signature,
        String signatureId,
        CertificateSource certificateSource,
        List<String> errors) {

        if (certificateSource == null) {
            log.warn(
                "No connector certificate source configured. Skipping certificate-in-source check "
                    + "for signature [{}]", signatureId
            );
            return;
        }

        var signingCertificateToken = signature.getSigningCertificateToken();
        if (signingCertificateToken == null) {
            errors.add("Signature [%s] has no signing certificate".formatted(signatureId));
            return;
        }

        var dssId = signingCertificateToken.getDSSId();
        var found = certificateSource.getCertificates()
                                     .stream()
                                     .anyMatch(cert -> cert.getDSSId().equals(dssId));

        if (found) {
            log.trace("Certificate [{}] found in the configured certificate source", dssId);
        } else {
            errors.add(
                "Signature [%s] certificate is not in the configured certificate source"
                    .formatted(signatureId));
        }
    }
}
