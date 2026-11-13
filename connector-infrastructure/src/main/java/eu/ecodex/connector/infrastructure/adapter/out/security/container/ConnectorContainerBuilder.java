/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.security.container;

import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.infrastructure.adapter.out.dss.ConnectorDssDocumentSigner;
import eu.ecodex.connector.infrastructure.adapter.out.dss.ConnectorDssSigningTokenProvider;
import eu.ecodex.connector.infrastructure.adapter.out.security.exception.ConnectorContainerException;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.container.ConnectorContainer;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.container.ConnectorContainerBusinessContent;
import eu.ecodex.connector.infrastructure.adapter.out.security.token.trustok.ConnectorTrustOKTokenGenerator;
import eu.ecodex.connector.infrastructure.adapter.out.security.token.validation.ConnectorTokenValidationGenerator;
import eu.ecodex.connector.infrastructure.adapter.out.security.token.validation.technical.ConnectorTokenValidationFactory;
import eu.ecodex.connector.infrastructure.config.property.container.ConnectorContainerProperties;
import eu.europa.esig.dss.enumerations.MimeType;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.spi.DSSUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Builder class responsible for creating containers from business messages.
 */
@Slf4j
@Component
public class ConnectorContainerBuilder {
    private final ConnectorTokenValidationFactory tokenValidationFactory;
    private final ConnectorTokenValidationGenerator validationTokenGenerator;
    private final ConnectorTrustOKTokenGenerator pdfTrustOKTokenGenerator;
    private final ConnectorTrustOKTokenGenerator xmlTrustOKTokenGenerator;
    private final ConnectorDssDocumentSigner connectorDssDocumentSigner;
    private final ConnectorFileStorageProvider fileStorageProvider;
    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorDssSigningTokenProvider signingTokenProvider;

    /**
     * Constructs a new instance of the ConnectorAsicContainerBuilder with the provided
     * dependencies.
     *
     * @param tokenValidationFactory     A factory for creating technical validation components
     *                                   needed for ASiC container construction.
     * @param validationTokenGenerator   A generator responsible for producing validation tokens.
     * @param pdfTrustOKTokenGenerator   A generator for creating TrustOK tokens specific to PDF
     *                                   documents.
     * @param xmlTrustOKTokenGenerator   A generator for creating TrustOK tokens specific to XML
     *                                   documents.
     * @param connectorDssDocumentSigner A signer responsible for applying digital signatures to
     *                                   documents.
     * @param fileStorageProvider        A provider for handling storage services for files during
     *                                   container creation.
     * @param attachmentRepository       A repository for managing message attachments used in
     *                                   container construction.
     * @param containerProperties        Configuration properties required for constructing ASiC
     *                                   containers.
     */
    public ConnectorContainerBuilder(
        ConnectorTokenValidationFactory tokenValidationFactory,
        ConnectorTokenValidationGenerator validationTokenGenerator,
        @Qualifier("connectorPDFTrustOKTokenGenerator")
        ConnectorTrustOKTokenGenerator pdfTrustOKTokenGenerator,
        @Qualifier("connectorXMLTrustOKTokenGenerator")
        ConnectorTrustOKTokenGenerator xmlTrustOKTokenGenerator,
        ConnectorDssDocumentSigner connectorDssDocumentSigner,
        ConnectorFileStorageProvider fileStorageProvider,
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorContainerProperties containerProperties) {
        this.tokenValidationFactory = tokenValidationFactory;
        this.validationTokenGenerator = validationTokenGenerator;
        this.pdfTrustOKTokenGenerator = pdfTrustOKTokenGenerator;
        this.xmlTrustOKTokenGenerator = xmlTrustOKTokenGenerator;
        this.connectorDssDocumentSigner = connectorDssDocumentSigner;
        this.fileStorageProvider = fileStorageProvider;
        this.attachmentRepository = attachmentRepository;

        var signature = containerProperties.getSignature();
        this.signingTokenProvider = new ConnectorDssSigningTokenProvider(
            signature.getKeystore(),
            signature.getPrivateKey()
        );
    }

    /**
     * Creates an ASiC container by validating the provided message, generating the necessary
     * tokens, and constructing the container with all required artefacts.
     *
     * @param message the input message containing business data and metadata required for container
     *                creation
     *
     * @return a {@code ConnectorContainer} instance containing the validated tokens, signed
     *     documents, and the constructed ASiC container
     *
     * @throws ConnectorContainerException if the business content or issuer validation fails
     */
    public ConnectorContainer createAsicContainer(ConnectorBusinessMessage message) {
        var businessContent = toContainerBusinessContent(message);

        if (!businessContent.isValid()) {
            throw new ConnectorContainerException("Invalid business content");
        }

        var issuer = tokenValidationFactory.getTokenIssuer(message);

        if (!issuer.isValid()) {
            throw new ConnectorContainerException("Invalid issuer");
        }

        var token = this.validationTokenGenerator.createToken(
            message,
            businessContent.getDocument(),
            businessContent.getDetachedSignature(),
            issuer
        );
        var pdfToken = this.pdfTrustOKTokenGenerator.generate(token);
        var xmlToken = this.xmlTrustOKTokenGenerator.generate(token);
        var asicDocument = this.createAsicDocument(businessContent, pdfToken);

        return ConnectorContainer.builder()
                                 .token(token)
                                 .tokenPDF(pdfToken)
                                 .tokenXML(xmlToken)
                                 .asicDocument(asicDocument).build();
    }

    private ConnectorContainerBusinessContent toContainerBusinessContent(
        @NonNull ConnectorBusinessMessage message) {
        var businessContent = message.businessContent();

        var containerBusinessContent = new ConnectorContainerBusinessContent();
        var businessDocument = businessContent.businessDocument();
        DSSDocument dssBusinessDocument;

        if (businessDocument != null) {
            var attachment = businessDocument.content();
            var content = this.fileStorageProvider.findByIdentifier(attachment.identifier());
            dssBusinessDocument = new InMemoryDocument(
                content,
                attachment.name(),
                MimeTypeEnum.PDF
            );
            // Detached signature is only applicable when a business document exists
            var detachedSignature = businessDocument.detachedSignature();

            if (detachedSignature != null) {
                var sigName = detachedSignature.name() != null
                    ? detachedSignature.name()
                    : "detachedSignature";
                var signature = this.fileStorageProvider.findByIdentifier(
                    detachedSignature.identifier()
                );
                containerBusinessContent.setDetachedSignature(
                    new InMemoryDocument(
                        signature,
                        sigName,
                        MimeType.fromMimeTypeString(detachedSignature.contentType())
                    )
                );
            }
        } else {
            // no business document - make XML to the main document
            log.warn("No business document content in message [{}]", message.identifier());
            log.warn("Making the XML content the main document");

            var xmlContent = this.fileStorageProvider.findByIdentifier(
                businessContent.xmlContent().identifier()
            );
            dssBusinessDocument = new InMemoryDocument(
                xmlContent,
                "mainDocument.xml",
                MimeTypeEnum.XML
            );
        }

        containerBusinessContent.setDocument(dssBusinessDocument);

        var attachments = this.attachmentRepository.findByMessageIdentifierAndTypes(
            message.identifier(),
            List.of(ConnectorAttachmentType.ATTACHMENT)
        );

        for (var attachment : attachments) {
            var mimeType = MimeType.fromMimeTypeString(attachment.contentType());
            log.debug(
                "Adding attachment [{}] ({}) to container",
                attachment.identifier(),
                attachment.contentType()
            );

            var content = this.fileStorageProvider.findByIdentifier(attachment.identifier());
            var attachmentDocument = new InMemoryDocument(content, attachment.name(), mimeType);
            containerBusinessContent.addAttachment(attachmentDocument);
        }

        return containerBusinessContent;
    }

    /**
     * Builds the ASiC-S ZIP content and signs it.
     *
     * <p>Note: the XML TrustOK token is intentionally excluded from the container per WP4 decision
     * it is shipped in parallel to the ASiC container, not embedded within it.
     * TODO: replace InMemoryDocument with a streaming document for large payloads.
     */
    private DSSDocument createAsicDocument(
        @NonNull ConnectorContainerBusinessContent businessContent,
        @NonNull DSSDocument trustOkTokenPDF) {
        var signedContentBytes = new ByteArrayOutputStream();

        try (var zipOutputStream = new ZipOutputStream(signedContentBytes)) {
            zipOutputStream.setLevel(ZipEntry.DEFLATED);

            // Put the main document
            writeZipEntry(
                zipOutputStream,
                resolveDocumentName(businessContent.getDocument(), "businessdocument"),
                businessContent.getDocument()
            );

            // add an optional detached signature
            if (businessContent.getDetachedSignature() != null) {
                writeZipEntry(
                    zipOutputStream,
                    resolveDocumentName(
                        businessContent.getDetachedSignature(),
                        "detachedsignature"
                    ),
                    businessContent.getDetachedSignature()
                );
            }

            // Put the attachments
            var attachments = businessContent.getAttachments();

            for (var i = 0; i < attachments.size(); i++) {
                var attachment = attachments.get(i);
                writeZipEntry(
                    zipOutputStream,
                    resolveDocumentName(attachment, "attachment_" + i),
                    attachment
                );
            }

            // Put the trustOkToken.pdf
            writeZipEntry(
                zipOutputStream,
                ConnectorContainerFileDefinitions.TOKEN_PDF_REF,
                trustOkTokenPDF
            );
        } catch (IOException e) {
            throw new ConnectorContainerException("Failed to build ASiC-S ZIP content", e);
        }

        // create asic-s container
        final var toBeSigned = new InMemoryDocument(
            signedContentBytes.toByteArray(),
            ConnectorContainerFileDefinitions.SIGNED_CONTENT_REF,
            MimeTypeEnum.BINARY
        );

        return this.connectorDssDocumentSigner.signWithASIC(toBeSigned, signingTokenProvider);
    }

    private void writeZipEntry(ZipOutputStream zip, String name, DSSDocument document)
        throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(DSSUtils.toByteArray(document.openStream()));
        zip.closeEntry();
    }

    private String resolveDocumentName(DSSDocument document, String fallback) {
        if (!StringUtils.hasLength(document.getName())) {
            log.warn("Document has no name, using fallback name [{}]", fallback);
            return fallback;
        }

        return document.getName();
    }
}
