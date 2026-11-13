/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.security.container.check;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeRepository;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingMode;
import eu.ecodex.connector.domain.model.security.ConnectorTruststore;
import eu.ecodex.connector.domain.model.security.KeystoreType;
import eu.ecodex.connector.infrastructure.adapter.out.dss.ConnectorDssCertificateSourceLoader;
import eu.ecodex.connector.infrastructure.adapter.out.dss.ConnectorDssCertificateVerifier;
import eu.ecodex.connector.infrastructure.adapter.out.security.container.checks.ConnectorContainerDocumentSignatureChecker;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.container.ConnectorContainer;
import eu.ecodex.connector.infrastructure.config.property.common.KeystoreProperties;
import eu.ecodex.connector.infrastructure.config.property.container.ConnectorContainerProperties;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.jaxb.object.Message;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import java.io.IOException;
import java.util.List;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorContainerDocumentSignatureChecker")
class ConnectorContainerDocumentSignatureCheckerTest {
    private static final String SIGNATURE_ID = "sig-1";
    private final ConnectorContainerProperties properties =
        mock(ConnectorContainerProperties.class, RETURNS_DEEP_STUBS);
    private final ConnectorBusinessDomainIdentifier businessDomainIdentifier =
        mock(ConnectorBusinessDomainIdentifier.class);
    private final ConnectorTruststore truststore =
        new ConnectorTruststore("truststore.jks", new byte[]{1}, "password", KeystoreType.JKS);
    private final DSSDocument tokenXml = new InMemoryDocument(new byte[]{1}, "token.xml");
    private final DSSDocument tokenPdf = new InMemoryDocument(new byte[]{2}, "token.pdf");
    private final DSSDocument asicDocument = new InMemoryDocument(new byte[]{3}, "asic.zip");
    @Mock
    private ConnectorProcessingModeRepository processingModeRepository;
    @Mock
    private ConnectorDssCertificateVerifier verifierFactory;
    @Mock
    private ConnectorDssCertificateSourceLoader sourceLoader;
    @Mock
    private ResourceLoader resourceLoader;

    private static AdvancedSignature signatureWithId() {
        var signature = mock(AdvancedSignature.class);
        when(signature.getId()).thenReturn(ConnectorContainerDocumentSignatureCheckerTest.SIGNATURE_ID);
        return signature;
    }

    private static Message message(String value) {
        var message = mock(Message.class);
        when(message.getValue()).thenReturn(value);
        return message;
    }

    private ConnectorContainerDocumentSignatureChecker newChecker() {
        return new ConnectorContainerDocumentSignatureChecker(
            processingModeRepository, verifierFactory, sourceLoader, properties);
    }

    private ConnectorContainer container() {
        return new ConnectorContainer(null, null, tokenXml, tokenPdf, asicDocument);
    }

    private void stubPipeline(Resource constraintsXml, KeyStoreCertificateSource source) {
        when(properties.getSignature().getValidation().getConstraintsXml())
            .thenReturn(constraintsXml);

        var processingMode = mock(ConnectorProcessingMode.class);
        when(processingMode.truststore()).thenReturn(truststore);
        when(processingModeRepository.findByBusinessDomainIdentifier(any()))
            .thenReturn(processingMode);

        when(sourceLoader.createCertificateSource(any(KeystoreProperties.class))).thenReturn(source);
        when(verifierFactory.createCommonCertificateVerifier(any(), any()))
            .thenReturn(mock(CommonCertificateVerifier.class));
    }

    private SignedDocumentValidator validatorWith(
        List<AdvancedSignature> signatures,
        SimpleReport simpleReport,
        DiagnosticData diagnosticData) {
        var validator = mock(SignedDocumentValidator.class);
        var reports = mock(Reports.class);
        when(validator.validateDocument()).thenReturn(reports);
        when(validator.getSignatures()).thenReturn(signatures);
        when(reports.getSimpleReport()).thenReturn(simpleReport);
        when(reports.getDiagnosticData()).thenReturn(diagnosticData);
        return validator;
    }

    private ThrowableAssert.ThrowingCallable checking(
        ConnectorContainerDocumentSignatureChecker checker, SignedDocumentValidator validator) {
        return () -> {
            try (var mocked = mockStatic(SignedDocumentValidator.class)) {
                mocked.when(() -> SignedDocumentValidator.fromDocument(any()))
                      .thenReturn(validator);
                checker.check(container(), businessDomainIdentifier);
            }
        };
    }

    @Nested
    @DisplayName("processing mode resolution")
    class ProcessingModeResolution {
        @Test
        void should_reject_when_no_processing_mode_exists_for_the_domain() {
            when(processingModeRepository.findByBusinessDomainIdentifier(any())).thenReturn(null);

            assertThatThrownBy(() -> newChecker().check(container(), businessDomainIdentifier))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
        }

        @Test
        void should_reject_when_the_processing_mode_has_no_truststore() {
            var processingMode = mock(ConnectorProcessingMode.class);
            when(processingMode.truststore()).thenReturn(null);
            when(processingModeRepository.findByBusinessDomainIdentifier(any()))
                .thenReturn(processingMode);

            assertThatThrownBy(() -> newChecker().check(container(), businessDomainIdentifier))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no truststore configured");
        }
    }

    @Nested
    @DisplayName("documents without signatures")
    class NoSignatures {
        @Test
        void should_reject_a_document_that_carries_no_signatures() {
            stubPipeline(null, null);
            var validator = mock(SignedDocumentValidator.class);
            when(validator.validateDocument()).thenReturn(mock(Reports.class));
            when(validator.getSignatures()).thenReturn(List.of());

            assertThatThrownBy(checking(newChecker(), validator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not contain any signature information");
        }
    }

    @Nested
    @DisplayName("signature validity")
    class SignatureValidity {
        @Test
        void should_accept_totally_passed_signatures_on_every_document() {
            stubPipeline(null, null);
            var simpleReport = mock(SimpleReport.class);
            when(simpleReport.getIndication(SIGNATURE_ID)).thenReturn(Indication.TOTAL_PASSED);
            var validator = validatorWith(
                List.of(signatureWithId()), simpleReport, mock(DiagnosticData.class));

            assertThatCode(checking(newChecker(), validator)).doesNotThrowAnyException();
        }

        @Test
        void should_report_qualification_errors_for_an_invalid_signature() {
            stubPipeline(null, null);
            var simpleReport = mock(SimpleReport.class);
            when(simpleReport.getIndication(SIGNATURE_ID)).thenReturn(Indication.FAILED);
            var qualificationErrors = List.of(message("format error"), message("policy error"));
            when(simpleReport.getQualificationErrors(SIGNATURE_ID))
                .thenReturn(qualificationErrors);
            var validator = validatorWith(
                List.of(signatureWithId()), simpleReport, mock(DiagnosticData.class));

            assertThatThrownBy(checking(newChecker(), validator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SIGNATURE_ID)
                .hasMessageContaining("format error/policy error");
        }

        @Test
        void should_report_the_algorithm_when_no_qualification_errors_are_present() {
            stubPipeline(null, null);
            var simpleReport = mock(SimpleReport.class);
            when(simpleReport.getIndication(SIGNATURE_ID)).thenReturn(Indication.FAILED);
            when(simpleReport.getQualificationErrors(SIGNATURE_ID)).thenReturn(List.of());
            var diagnosticData = mock(DiagnosticData.class);
            when(diagnosticData.getSignatureEncryptionAlgorithm(SIGNATURE_ID))
                .thenReturn(EncryptionAlgorithm.RSA);
            when(diagnosticData.getSignatureDigestAlgorithm(SIGNATURE_ID))
                .thenReturn(DigestAlgorithm.SHA256);
            var validator = validatorWith(
                List.of(signatureWithId()), simpleReport, diagnosticData);

            assertThatThrownBy(checking(newChecker(), validator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid verification result for algorithm");
        }
    }

    @Nested
    @DisplayName("certificate source")
    class CertificateSourceCheck {
        @Test
        void should_report_a_signature_without_a_signing_certificate() {
            stubPipeline(null, mock(KeyStoreCertificateSource.class));
            var signature = signatureWithId();
            when(signature.getSigningCertificateToken()).thenReturn(null);
            var simpleReport = mock(SimpleReport.class);
            when(simpleReport.getIndication(SIGNATURE_ID)).thenReturn(Indication.TOTAL_PASSED);
            var validator = validatorWith(
                List.of(signature), simpleReport, mock(DiagnosticData.class));

            assertThatThrownBy(checking(newChecker(), validator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no signing certificate");
        }

        @Test
        void should_report_a_signing_certificate_absent_from_the_source() {
            var source = mock(KeyStoreCertificateSource.class);
            when(source.getCertificates()).thenReturn(List.of());
            stubPipeline(null, source);
            var signature = signatureWithId();
            when(signature.getSigningCertificateToken()).thenReturn(mock(CertificateToken.class));
            var simpleReport = mock(SimpleReport.class);
            when(simpleReport.getIndication(SIGNATURE_ID)).thenReturn(Indication.TOTAL_PASSED);
            var validator = validatorWith(
                List.of(signature), simpleReport, mock(DiagnosticData.class));

            assertThatThrownBy(checking(newChecker(), validator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in the configured certificate source");
        }
    }

    @Nested
    @DisplayName("validation constraints")
    class ValidationConstraints {
        @Test
        void should_wrap_an_io_error_while_reading_the_constraints() throws IOException {
            var resource = mock(Resource.class);
            lenient().when(resource.getInputStream()).thenThrow(new IOException("boom"));
            lenient().when(resourceLoader.getResource("classpath:policy.xml")).thenReturn(resource);

            stubPipeline(new ClassPathResource("classpath:policy.xml"), null);

            assertThatThrownBy(checking(newChecker(), mock(SignedDocumentValidator.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot open signature validation constraints");
        }
    }

    @Nested
    @DisplayName("invalid input")
    @SuppressWarnings("DataFlowIssue")
    class InvalidInput {
        @Test
        void should_reject_a_null_container() {
            assertThatThrownBy(() -> newChecker().check(null, businessDomainIdentifier))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void should_reject_a_null_business_domain_identifier() {
            assertThatThrownBy(() -> newChecker().check(container(), null))
                .isInstanceOf(NullPointerException.class);
        }
    }
}
