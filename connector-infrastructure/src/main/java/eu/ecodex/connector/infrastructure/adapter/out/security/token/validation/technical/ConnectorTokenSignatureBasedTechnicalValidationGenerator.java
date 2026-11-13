/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.security.token.validation.technical;

import eu.ecodex.connector.infrastructure.adapter.out.dss.ConnectorDssCertificateSourceLoader;
import eu.ecodex.connector.infrastructure.adapter.out.dss.ConnectorDssCertificateVerifier;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.DecisionData;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.DiagnosisData;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.ValidationData;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.ConnectorTokenOriginalValidationReportContainer;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.ConnectorTokenTechnicalTrustLevel;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.ConnectorTokenValidation;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.ConnectorTokenVerificationData;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.signature.ConnectorTokenCertificateInformation;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.signature.ConnectorTokenSignature;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.signature.ConnectorTokenSignatureInformation;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.signature.ConnectorTokenTechnicalValidationResult;
import eu.ecodex.connector.infrastructure.adapter.out.security.util.TechnicalValidationUtil;
import eu.ecodex.connector.infrastructure.config.property.businessdocument.ConnectorBusinessDocumentProperties;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.enumerations.SignatureQualification;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.signature.DefaultSignatureProcessExecutor;
import eu.europa.esig.dss.validation.reports.Reports;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link ConnectorTokenTechnicalValidationGenerator} responsible for
 * performing DSS-based technical validation of signed business documents.
 *
 * <p>This implementation uses the DSS (Digital Signature Services) framework to:
 * <ul>
 *   <li>Validate embedded or detached signatures</li>
 *   <li>Analyse certificate chains and revocation status</li>
 *   <li>Determine a technical trust level ({@link ConnectorTokenTechnicalTrustLevel})</li>
 *   <li>Populate {@link ConnectorTokenValidation} with signature and certificate details</li>
 *   <li>Attach the original DSS validation reports for traceability</li>
 * </ul>
 *
 * <p>The validation process follows a rule-based decision model:
 * <ul>
 *   <li>
 *       <b>FAIL (RED)</b>: Any critical validation error (invalid signature,
 *       certificate issues, etc.)
 *   </li>
 *   <li><b>SUFFICIENT (YELLOW)</b>: Partial validation (e.g. missing OCSP/CRL data)</li>
 *   <li><b>SUCCESSFUL (GREEN)</b>: Fully valid signature and certificate chain</li>
 * </ul>
 *
 * <p>Signatures associated with certificates present in the configured ignored store
 * are excluded from validation results.
 *
 * <p>In case of validation errors or exceptions:
 * <ul>
 *   <li>The validation result is set to {@code FAIL}</li>
 *   <li>A fallback verification time is assigned if missing</li>
 *   <li>An empty {@link ConnectorTokenOriginalValidationReportContainer} is created if needed</li>
 * </ul>
 */
@Slf4j
@Component
public class ConnectorTokenSignatureBasedTechnicalValidationGenerator implements
    ConnectorTokenTechnicalValidationGenerator {
    private final ConnectorDssCertificateVerifier dssCertificateVerifier;
    private final ConnectorDssCertificateSourceLoader dssCertificateSourceLoader;
    private final ConnectorBusinessDocumentProperties businessDocumentProperties;
    private final CertificateSource ignoredCertificateStore;

    /**
     * Constructs an instance of ConnectorContainerDefaultTechnicalValidationGenerator.
     *
     * @param dssCertificateVerifier     the certificate verifier used for validation processes.
     * @param dssCertificateSourceLoader the source loader responsible for loading certificate
     *                                   sources.
     * @param businessDocumentProperties the business document properties configuration.
     */
    public ConnectorTokenSignatureBasedTechnicalValidationGenerator(
        ConnectorDssCertificateVerifier dssCertificateVerifier,
        ConnectorDssCertificateSourceLoader dssCertificateSourceLoader,
        ConnectorBusinessDocumentProperties businessDocumentProperties) {
        this.dssCertificateVerifier = dssCertificateVerifier;
        this.dssCertificateSourceLoader = dssCertificateSourceLoader;
        this.businessDocumentProperties = businessDocumentProperties;
        this.ignoredCertificateStore = loadIgnoredCertificateStore();
    }

    @Override
    public ConnectorTokenValidation generate(
        DSSDocument businessDocument,
        DSSDocument detachedSignature) throws Exception {
        log.debug("Creating token validation");

        var tokenValidation = new ConnectorTokenValidation();
        var technicalResult = new ConnectorTokenTechnicalValidationResult();
        var verificationData = new ConnectorTokenVerificationData();
        verificationData.setAuthenticationData(null);
        verificationData.setSignatureData(new ArrayList<>());
        tokenValidation.setTechnicalResult(technicalResult);
        tokenValidation.setVerificationData(verificationData);

        try {
            var validator = createValidator(businessDocument, detachedSignature);
            // Validate the document and generate the validation report
            log.debug("Validating the document");

            var policy = businessDocumentProperties.getSignature()
                                                   .getValidation()
                                                   .getConstraintsXml();
            var reports = validator.validateDocument(policy.getInputStream());
            var simpleReport = reports.getSimpleReport();
            var detailedReport = reports.getDetailedReport();
            log.debug("DSS validation simple report: [{}]", simpleReport);
            log.debug("DSS validation detailed report: [{}]", detailedReport);

            var verificationTime = getVerificationTime(simpleReport);
            tokenValidation.setVerificationTime(verificationTime);

            // Filter out XAdES signatures — detected by DSS but not providing enough information
            var signatures = validator.getSignatures()
                                      .stream()
                                      .filter(sig -> {
                                          var fmt = simpleReport.getSignatureFormat(sig.getId());
                                          return fmt == null || !SignatureForm.XAdES.equals(
                                              fmt.getSignatureForm());
                                      })
                                      .collect(Collectors.toCollection(ArrayList::new));
            // inspect the info and derive some diagnosis and validation data
            final var diagnosticData = reports.getDiagnosticData();
            var idsToRemove = new ArrayList<String>();

            for (var signature : signatures) {
                var publicKey = signature.getSigningCertificateToken().getCertificate()
                                         .getPublicKey();
                if (ignoredCertificateStore != null
                    && !ignoredCertificateStore.getByPublicKey(publicKey).isEmpty()) {
                    log.debug(
                        "Ignoring signature [{}] — certificate is on the ignore list",
                        signature.getId()
                    );
                    idsToRemove.add(signature.getId());
                    diagnosticData.getSignatureIdList().remove(signature.getId());
                } else {
                    var decisionData = computeDecisionData(signature, reports);
                    determineDecision(decisionData, tokenValidation);

                    // propagate to token validation instance
                    var diagnosis = decisionData.getDiagnosis();
                    var validation = decisionData.getValidation();

                    var certInfo = new ConnectorTokenCertificateInformation();
                    certInfo.setSubject(diagnosis.getSigningCertificateSubject());
                    certInfo.setIssuer(diagnosis.getSigningCertificateIssuer());
                    certInfo.setValid(
                        validation.getSignatureCertStatus()
                            != ConnectorTokenTechnicalTrustLevel.FAIL
                    );
                    certInfo.setValidityAtSigningTime(
                        validation.getSignatureCertHistory()
                            != ConnectorTokenTechnicalTrustLevel.FAIL
                    );

                    var signatureInfo = new ConnectorTokenSignatureInformation();
                    signatureInfo.setFormat(diagnosis.getSignatureFormatLevel());
                    signatureInfo.setLevel(diagnosis.getSignatureConclusion().name());
                    signatureInfo.setSignatureValid(validation.isSignatureComputation());
                    // Structure is always valid if DSS produced a report with signature info
                    signatureInfo.setStructureValid(true);

                    var sigTechnicalResult = new ConnectorTokenTechnicalValidationResult();
                    sigTechnicalResult.setTrustLevel(diagnosis.getTrustLevel());
                    sigTechnicalResult.setComment(diagnosis.getComment());

                    var tokenSignature = new ConnectorTokenSignature();
                    tokenSignature.setSigningTime(diagnosis.getSigningTime());
                    tokenSignature.setCertificateInformation(certInfo);
                    tokenSignature.setSignatureInformation(signatureInfo);
                    tokenSignature.setTechnicalResult(sigTechnicalResult);

                    tokenValidation.getVerificationData().addSignatureData(tokenSignature);
                }
            }

            if (signatures.isEmpty() || idsToRemove.size() == signatures.size()) {
                log.warn("No valid signatures found in DSS report — setting FAIL level");
                technicalResult.setTrustLevel(ConnectorTokenTechnicalTrustLevel.FAIL);
                technicalResult.setComment("Unable to find a signature.");
                // Backward compatibility: at least one signature entry must be present
                tokenValidation.getVerificationData().addSignatureData(
                    new ConnectorTokenSignature()
                );
            }

            var technicalValidationResult = tokenValidation.getTechnicalResult();
            log.info(
                "General result determined to lowest level: {}: {}",
                technicalValidationResult.getTrustLevel(),
                technicalValidationResult.getComment()
            );

            removeSignaturesFromSimpleReport(simpleReport, idsToRemove);

            // Add the original report to the token validation
            log.debug("Propagating DSS validation report");
            var reportContainer = new ConnectorTokenOriginalValidationReportContainer();
            reportContainer.setReports(reports);
            tokenValidation.setOriginalValidationReport(reportContainer);

            return tokenValidation;
        } catch (Exception e) {
            log.error("Error during technical validation", e);
            // => set also the verification time if needed
            if (tokenValidation.getVerificationTime() == null) {
                try {
                    tokenValidation.setVerificationTime(
                        DatatypeFactory.newInstance()
                                       .newXMLGregorianCalendar(new GregorianCalendar())
                    );
                } catch (DatatypeConfigurationException ex) {
                    log.warn("Failed to set fallback verification time", ex);
                }
            }

            // => set also the original validation report if needed
            if (tokenValidation.getOriginalValidationReport() == null) {
                tokenValidation.setOriginalValidationReport(
                    new ConnectorTokenOriginalValidationReportContainer()
                );
            }

            technicalResult.setTrustLevel(ConnectorTokenTechnicalTrustLevel.FAIL);
            technicalResult.setComment(
                "An error occurred, while validating the signature via DSS."
            );

            log.warn(
                "Validation failed with exception: result set to [{}] — [{}]",
                technicalResult.getTrustLevel(), technicalResult.getComment()
            );

            return tokenValidation;
        }
    }

    @Override
    public boolean supportsAuthenticationBased() {
        return false;
    }

    private CertificateSource loadIgnoredCertificateStore() {
        var validation = businessDocumentProperties.getSignature().getValidation();

        if (validation.isIgnoreStoreEnabled()) {
            return dssCertificateSourceLoader.createCertificateSource(validation.getIgnoreStore());
        }

        return null;
    }

    private SignedDocumentValidator createValidator(
        DSSDocument businessDocument, DSSDocument detachedSignature) {
        final SignedDocumentValidator validator;

        if (detachedSignature != null) {
            log.debug("Using detached signature validator");
            validator = SignedDocumentValidator.fromDocument(detachedSignature);
            validator.setDetachedContents(List.of(businessDocument));
        } else {
            log.debug("Using embedded signature validator");
            validator = SignedDocumentValidator.fromDocument(businessDocument);
        }

        validator.setProcessExecutor(new DefaultSignatureProcessExecutor());
        validator.setCertificateVerifier(dssCertificateVerifier.createCommonCertificateVerifier(
            businessDocumentProperties.getSignature().getValidation()
        ));

        return validator;
    }

    private XMLGregorianCalendar getVerificationTime(SimpleReport simpleReport)
        throws DatatypeConfigurationException {
        var factory = DatatypeFactory.newInstance(); // create once

        if (simpleReport.getValidationTime() != null) {
            log.debug("Validation took [{}] ms", simpleReport.getValidationTime());
            var gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(simpleReport.getValidationTime());

            return factory.newXMLGregorianCalendar(gregorianCalendar);
        }

        log.debug("No validation time in the DSS report — using current time");

        return factory.newXMLGregorianCalendar(new GregorianCalendar());
    }

    private @NonNull DecisionData computeDecisionData(
        @NonNull AdvancedSignature signature,
        @NonNull Reports reports) {
        log.info("Computing the diagnosis data");
        var diagnosticData = reports.getDiagnosticData();
        var simpleReport = reports.getSimpleReport();
        var signatureId = signature.getId();
        var certificateId = diagnosticData.getSigningCertificateId(signatureId);

        // compute some diagnosis data
        var signingTime = TechnicalValidationUtil.getSigningTime(signature);
        var signingCertToken = TechnicalValidationUtil.getCertificateToken(
            signature,
            certificateId
        );
        var signingCertificate = TechnicalValidationUtil.getCertificate(signingCertToken);
        var signingCertSubject = TechnicalValidationUtil.getCertificateSubjectName(
            signingCertificate
        );
        var signingCertIssuer = TechnicalValidationUtil.getCertificateIssuerName(
            signingCertificate);
        var signatureFormatLevel = TechnicalValidationUtil.getSignatureFormatLevelAsString(
            simpleReport,
            signatureId
        ); // PAdES-BES etc

        var signatureConclusion = TechnicalValidationUtil.getSignatureConclusion(
            simpleReport, signatureId
        ); // QES etc

        var issuerCertWrapper = TechnicalValidationUtil.getCertificateWrapper(
            diagnosticData.getUsedCertificates(), signingCertIssuer);

        var issuingCertToken = TechnicalValidationUtil.getCertificateToken(
            signature, issuerCertWrapper.getId());
        var issuingCertificate = TechnicalValidationUtil.getCertificate(issuingCertToken);

        // compute some validation attributes
        boolean validSignatureComputation = TechnicalValidationUtil.checkSignatureCorrectness(
            simpleReport, signatureId);
        boolean validSignatureConclusion = TechnicalValidationUtil.checkSignatureConclusion(
            simpleReport, reports.getDetailedReport(), signatureId);
        boolean validSignatureFormat = StringUtils.hasText(signatureFormatLevel);

        var validSignatureCertStatus = TechnicalValidationUtil.checkCertificateRevocation(
            signingCertToken,
            diagnosticData.getCertificateRevocationStatus(certificateId).isRevoked()
        );
        var validSignatureCertHistory = TechnicalValidationUtil.checkCertificateValidity(
            signingCertToken, signingTime);
        boolean validTrustAnchor = TechnicalValidationUtil.checkTrustAnchor(
            diagnosticData, certificateId);
        var validIssuerCertStatus = TechnicalValidationUtil.checkCertificateRevocation(
            issuingCertToken);
        var validIssuerCertHistory = TechnicalValidationUtil.checkCertificateValidity(
            issuingCertToken, signingTime);

        log.debug(
            "Decision data: signingTime={}, format={}, subject={}, issuer={}, "
                + "conclusion={}, sigComputation={}, certStatus={}, certHistory={}, "
                + "trustAnchor={}, sigConclusion={}, sigFormat={}, "
                + "issuerStatus={}, issuerHistory={}",
            signingTime, signatureFormatLevel, signingCertSubject, signingCertIssuer,
            signatureConclusion, validSignatureComputation, validSignatureCertStatus,
            validSignatureCertHistory, validTrustAnchor, validSignatureConclusion,
            validSignatureFormat, validIssuerCertStatus, validIssuerCertHistory
        );

        // create the data container and put it in the cache
        var diagnosis = new DiagnosisData(
            signingTime,
            signingCertificate,
            signingCertIssuer,
            signingCertSubject,
            signatureFormatLevel,
            signatureConclusion,
            issuingCertificate,
            ConnectorTokenTechnicalTrustLevel.FAIL,
            "Not yet determined!"
        );
        var validation = new ValidationData(
            validSignatureComputation,
            validSignatureConclusion,
            validSignatureFormat,
            validSignatureCertStatus,
            validSignatureCertHistory,
            validTrustAnchor,
            validIssuerCertStatus,
            validIssuerCertHistory
        );

        return new DecisionData(diagnosis, validation);
    }

    private void determineDecision(
        @NonNull DecisionData decisionData,
        @NonNull ConnectorTokenValidation tokenValidation) {
        var validation = decisionData.getValidation();

        // PART A: FAIL checks — any failure here exits immediately

        log.debug("Decision PART A: FAIL checks");
        // 1. The signature has to be mathematically correct. Otherwise, the result of the technical
        // validation has to be RED
        if (!validation.isSignatureComputation()) {
            decide(
                decisionData,
                ConnectorTokenTechnicalTrustLevel.FAIL,
                "The signature is not mathematically correct.",
                tokenValidation
            );
            return;
        }
        // 2. If DSS can recognize and analyse the signature format (e.g. PAdES-BES), the final
        // conclusion can be GREEN, otherwise the final conclusion will be RED
        if (!validation.isSignatureFormat()) {
            decide(
                decisionData,
                ConnectorTokenTechnicalTrustLevel.FAIL,
                "The signature format could not be detected.",
                tokenValidation
            );
            return;
        }
        // 3. QES, ADES_QC and ADES are allowed signature levels and can create a GREEN result,
        // an UNDETERMINED signature level will not be allowed, and the final conclusion will be RED
        if (!validation.isSignatureConclusion()) {
            decide(
                decisionData,
                ConnectorTokenTechnicalTrustLevel.FAIL,
                "The signature conclusion is not sufficient.",
                tokenValidation
            );
            return;
        }
        // 4. The signing certificate at least has to be valid at the time of signing
        // (not revoked, not expired, recognizable by DSS).
        if (validation.getSignatureCertStatus() == ConnectorTokenTechnicalTrustLevel.FAIL
            || validation.getSignatureCertHistory() == ConnectorTokenTechnicalTrustLevel.FAIL) {
            decide(
                decisionData,
                ConnectorTokenTechnicalTrustLevel.FAIL,
                "The signature certificate is not valid at the time of signing "
                    + "(non-active or revoked).",
                tokenValidation
            );
            return;
        }

        // 5. The only certificate with the need to be checked is the issuing certificate for
        // the signing certificate.
        //    A validation down to a root certificate is not necessary.
        //    For this certificate, the same rules apply as they do for the signing certificate,
        //    with the addition that the issuing certificate had to be valid at the time the
        //    signing certificate started to become valid.

        // checks that the signing certificate has been signed by the issuer certificate
        if (validation.getIssuerCertStatus() == ConnectorTokenTechnicalTrustLevel.FAIL) {
            decide(
                decisionData,
                ConnectorTokenTechnicalTrustLevel.FAIL,
                "The issuer certificate could not be detected or is invalid.",
                tokenValidation
            );
            return;
        }
        if (validation.getIssuerCertHistory() != ConnectorTokenTechnicalTrustLevel.SUCCESSFUL) {
            // there is no SUFFICIENT for this check
            decide(
                decisionData,
                ConnectorTokenTechnicalTrustLevel.FAIL,
                "The issuer certificate is not valid at the time of signing "
                    + "(revoked, expired or not recognisable).",
                tokenValidation
            );
            return;
        }

        // PART B: SUFFICIENT checks
        log.debug("Decision PART B: SUFFICIENT checks");

        // 4-3. Being valid at the time of signing with an CRL and/or an OCSP being defined, but
        // none of them is reachable: YELLOW
        if (validation.getSignatureCertStatus() == ConnectorTokenTechnicalTrustLevel.SUFFICIENT
            || validation.getSignatureCertHistory()
            == ConnectorTokenTechnicalTrustLevel.SUFFICIENT) {
            decide(
                decisionData,
                ConnectorTokenTechnicalTrustLevel.SUFFICIENT,
                "The signature certificate's validity at the time of signing could "
                    + "not be fully determined (OCSP/CRL data not available).",
                tokenValidation
            );
            return;
        }

        var diagnosis = decisionData.getDiagnosis();

        if (diagnosis.getSignatureConclusion() == SignatureQualification.QESIG
            && !validation.isTrustAnchor()) {
            // 5-1. In the case of qualified signatures, the issuing certificate has to be present
            // and verifiable at a national TSL.
            //      Otherwise, the signature can be assessed with “AdES_QC” and the comment “Unable
            //      to verify the certificate issuer at a national TSL”.
            var sigData = tokenValidation.getVerificationData().getSignatureData();
            if (!sigData.isEmpty()) {
                sigData.getFirst().getSignatureInformation()
                       .setLevel(SignatureQualification.ADESIG_QC.name());
            } else {
                log.warn("Cannot downgrade signature level — no signature data present");
            }
            decide(
                decisionData,
                ConnectorTokenTechnicalTrustLevel.SUFFICIENT,
                "Unable to verify the certificate's issuer at a national TSL.",
                tokenValidation
            );
            return;
        } else if (diagnosis.getSignatureConclusion() == SignatureQualification.ADESIG_QC) {
            log.info(
                "ADESIG_QC: TSL check is informational only — does not affect the result "
                    + "(WP4 spec)"
            );
            // noinspection ConstantConditions
            // only for clarity
            // ADESIG_QC: TSL check is informational only — does not affect the result (WP4 spec)

            // 5-2. In the case of AdES and AdES_QC, the issuing certificate should be validated
            // against a TSL if possible,
            //      and the result of the verification should be part of the technical
            //      validation part of the “Trust OK”-Token.
            //      The outcome of this validation thereby does not affect the result of
            //      the technical validation.
        }

        // PART C: SUCCESSFUL
        log.debug("Decision PART C: SUCCESSFUL");

        // passed all the previous checks
        decide(
            decisionData,
            ConnectorTokenTechnicalTrustLevel.SUCCESSFUL,
            "The signature is valid.",
            tokenValidation
        );
    }

    /**
     * Applies a trust decision to the diagnosis and updates the token's overall trust level using a
     * lowest-wins strategy: SUCCESSFUL → SUFFICIENT → FAIL.
     */
    private void decide(
        DecisionData decisionData,
        ConnectorTokenTechnicalTrustLevel trustLevel,
        String comment,
        ConnectorTokenValidation tokenValidation) {
        var diagnosis = decisionData.getDiagnosis();

        if (diagnosis == null) {
            log.warn("Cannot apply trust decision — DiagnosisData is null");
            return;
        }

        diagnosis.setTrustLevel(trustLevel);
        diagnosis.setComment(comment);
        log.debug("Trust decision: [{}] — {}", trustLevel, comment);

        var overall = tokenValidation.getTechnicalResult();
        var current = overall.getTrustLevel();

        boolean shouldDowngrade = current == null
            || (current == ConnectorTokenTechnicalTrustLevel.SUCCESSFUL
            && (trustLevel == ConnectorTokenTechnicalTrustLevel.SUFFICIENT
            || trustLevel == ConnectorTokenTechnicalTrustLevel.FAIL))
            || (current == ConnectorTokenTechnicalTrustLevel.SUFFICIENT
            && trustLevel == ConnectorTokenTechnicalTrustLevel.FAIL);

        if (shouldDowngrade) {
            overall.setTrustLevel(trustLevel);
            overall.setComment(comment);
        }
    }

    private void removeSignaturesFromSimpleReport(
        SimpleReport simpleReport, List<String> idsToRemove) {
        if (idsToRemove.isEmpty()) {
            return;
        }

        var tokens = simpleReport.getJaxbModel().getSignatureOrTimestampOrEvidenceRecord();

        tokens.removeIf(token -> idsToRemove.contains(token.getId()));
        simpleReport.getSignatureIdList().removeIf(idsToRemove::contains);
    }
}
