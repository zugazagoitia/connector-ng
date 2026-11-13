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
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageBusinessContentRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorBusinessDocumentAESType;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessDocument;
import eu.ecodex.connector.domain.model.message.content.DetachedSignatureMimeType;
import eu.ecodex.connector.infrastructure.adapter.out.security.container.checks.ConnectorContainerDocumentSignatureChecker;
import eu.ecodex.connector.infrastructure.adapter.out.security.container.checks.ConnectorMessageContainerChecker;
import eu.ecodex.connector.infrastructure.adapter.out.security.exception.ConnectorContainerException;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.container.ConnectorContainer;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.container.ConnectorContainerBusinessContent;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.token.ConnectorToken;
import eu.ecodex.connector.infrastructure.adapter.out.security.util.XMLStreamUtil;
import eu.ecodex.connector.infrastructure.adapter.out.security.util.ZipStreamUtil;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.spi.DSSUtils;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The ConnectorAsicContainerValidator is responsible for validating containers in incoming
 * messages. It ensures that the provided message contains valid ASIC-S and XML trust OK token
 * attachments and processes the container contents. This class interacts with a file storage
 * provider to retrieve attachment contents, validates the structure and type of the container, and
 * persists its contents as necessary.
 */
@Slf4j
@Component
public class ConnectorContainerValidator {
    private final List<ConnectorMessageContainerChecker> checkers;
    private final ConnectorContainerDocumentSignatureChecker signatureChecker;
    private final ConnectorMessageBusinessContentRepository businessContentRepository;
    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorFileStorageProvider fileStorageProvider;

    /**
     * Constructor for the ConnectorAsicContainerValidator.
     *
     * @param checkers                  the list of checkers to be used for validating the
     *                                  container
     * @param signatureChecker          the signature checker responsible for validating the
     *                                  document signature
     * @param businessContentRepository the repository for managing and querying the business
     *                                  content of a connector message
     * @param attachmentRepository      the repository for managing and querying attachments
     *                                  associated with a connector message
     * @param fileStorageProvider       the service provider responsible for storage operations
     *                                  related to attachments and metadata
     */
    public ConnectorContainerValidator(
        List<ConnectorMessageContainerChecker> checkers,
        ConnectorContainerDocumentSignatureChecker signatureChecker,
        ConnectorMessageBusinessContentRepository businessContentRepository,
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorFileStorageProvider fileStorageProvider) {
        this.checkers = checkers;
        this.signatureChecker = signatureChecker;
        this.businessContentRepository = businessContentRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageProvider = fileStorageProvider;
    }

    /**
     * Validates the presence and integrity of the required attachments of the given business
     * message, namely the ASiC-S container and the XML trust token.
     *
     * <p>The method checks that the attachments are retrievable from storage, processes their
     * contents, and persists the extracted content.
     *
     * @param message the business message whose attachments are validated (must not be null)
     *
     * @return the {@link ConnectorBusinessMessage} after validation, with the extracted content
     *     applied;
     *
     * @throws IllegalArgumentException    if the message has no attachments or lacks one of the
     *                                     required attachment types
     * @throws ConnectorContainerException if an error occurs while processing the attachments, such
     *                                     as an I/O error or an XML parsing error
     * @throws NullPointerException        if {@code message} is {@code null}
     */
    public ConnectorBusinessMessage validate(@NonNull ConnectorBusinessMessage message) {
        log.info("Validating ASIC-S container for message {}", message.identifier());
        var attachments = message.attachments();

        if (attachments == null || attachments.isEmpty()) {
            throw new IllegalArgumentException("Message must have at least one attachment");
        }

        var asicAttachment = findAttachmentByType(attachments, ConnectorAttachmentType.ASICS)
            .orElseThrow(() -> new IllegalArgumentException(
                "Message must have an ASIC-S attachment"));

        var xmlTokenAttachment = findAttachmentByType(
            attachments,
            ConnectorAttachmentType.XML_TOKEN
        )
            .orElseThrow(() -> new IllegalArgumentException(
                "Message must have an XML trust OK token attachment"));

        final ConnectorContainer container;

        try {
            var asicsBytes = fileStorageProvider.findByIdentifier(asicAttachment.identifier());
            var xmlTokenBytes = fileStorageProvider.findByIdentifier(
                xmlTokenAttachment.identifier());
            container = buildContainer(asicsBytes, xmlTokenBytes);
            checkContainer(container, message);
        } catch (IOException | JAXBException | RuntimeException e) {
            throw new ConnectorContainerException(
                "Failed to load or parse incoming connector container", e
            );
        }

        try {
            return persistAsicsContent(container, message);
        } catch (IOException e) {
            // TODO: remove ASIC-S from S3 bucket on failure
            throw new ConnectorContainerException("Failed to persist ASiC-S container content", e);
        }
    }

    protected ConnectorContainer buildContainer(byte[] asicsBytes, byte[] xmlTokenBytes)
        throws IOException, JAXBException {
        var asicsDocument = new InMemoryDocument(
            asicsBytes,
            ConnectorContainerFileDefinitions.SIGNED_CONTENT_ASIC_REF
        );

        var xmlTokenDocument = new InMemoryDocument(
            xmlTokenBytes,
            ConnectorContainerFileDefinitions.TOKEN_XML_REF
        );

        if (DSSUtils.isEmpty(asicsDocument)) {
            throw new ConnectorContainerException("ASiC-S container is empty");
        }

        if (DSSUtils.isEmpty(xmlTokenDocument)) {
            throw new ConnectorContainerException("XML trust OK token is empty");
        }

        if (!ZipStreamUtil.isZipFile(asicsDocument)) {
            throw new ConnectorContainerException("ASiC-S container is not a ZIP file");
        }

        if (!XMLStreamUtil.isXmlFile(xmlTokenDocument)) {
            throw new ConnectorContainerException("XML trust OK token is not an XML file");
        }

        ConnectorToken token;

        try (var xmlIn = xmlTokenDocument.openStream()) {
            token = XMLStreamUtil.decodeXMLStream(xmlIn);
        }

        var businessDocumentName = token.getDocument().getFilename();
        var detachedSignatureName = token.getDocument().getSignatureFilename();

        var asicsContents = ZipStreamUtil.extract(asicsDocument);
        var businessContent = new ConnectorContainerBusinessContent();
        DSSDocument pdfTrustOKToken = null;

        for (var asicsContent : asicsContents) {
            log.debug("ASiC-S entry: {}", asicsContent.getName());
            if (!ConnectorContainerFileDefinitions.SIGNED_CONTENT_REF
                .equalsIgnoreCase(asicsContent.getName())) {
                continue;
            }
            pdfTrustOKToken = classifyZipEntries(
                ZipStreamUtil.extract(asicsContent),
                businessContent,
                businessDocumentName,
                detachedSignatureName
            );
        }

        if (pdfTrustOKToken == null) {
            throw new ConnectorContainerException(
                "ASiC-S signed content does not contain a PDF trust OK token");
        }

        return new ConnectorContainer(
            businessContent,
            token,
            xmlTokenDocument,
            pdfTrustOKToken,
            asicsDocument
        );
    }

    /**
     * Routes each entry in the signed content ZIP into its destination slot. Returns the PDF token
     * document, or null if not present (caller must validate).
     */
    private DSSDocument classifyZipEntries(
        List<DSSDocument> entries,
        ConnectorContainerBusinessContent businessContent,
        String businessDocumentName,
        String detachedSignatureName) {
        DSSDocument pdfToken = null;

        for (var entry : entries) {
            var name = entry.getName();
            log.debug("Signed content entry: {}", name);

            if (ConnectorContainerFileDefinitions.TOKEN_PDF_REF.equalsIgnoreCase(name)) {
                pdfToken = entry;
            } else if (StringUtils.hasText(businessDocumentName)
                && businessDocumentName.equalsIgnoreCase(name)) {
                businessContent.setDocument(entry);
            } else if (StringUtils.hasText(detachedSignatureName)
                && detachedSignatureName.equalsIgnoreCase(name)) {
                businessContent.setDetachedSignature(entry);
            } else {
                businessContent.addAttachment(entry);
            }
        }

        return pdfToken;
    }

    private Optional<ConnectorMessageAttachment> findAttachmentByType(
        List<ConnectorMessageAttachment> attachments,
        ConnectorAttachmentType type) {
        return attachments.stream()
                          .filter(a -> type.equals(a.type()))
                          .findFirst();
    }

    private ConnectorBusinessMessage persistAsicsContent(
        ConnectorContainer container,
        ConnectorBusinessMessage message)
        throws IOException {
        var attachments = message.attachments() != null
            ? new ArrayList<>(message.attachments())
            : new ArrayList<ConnectorMessageAttachment>();
        attachments.removeIf(a -> a.type() == ConnectorAttachmentType.ASICS);

        attachments.add(persistPdfTrustToken(container, message));
        attachments.addAll(persistAdditionalAttachments(container, message));

        var businessDocument = persistBusinessDocument(container, message);
        var businessContent = message.businessContent().toBuilder()
                                     .businessDocument(businessDocument)
                                     .build();

        // update the message business document
        // this is only possible there because it is contained in the ASIC-S container
        return message.toBuilder()
                      .businessContent(businessContent)
                      .attachments(attachments)
                      .build();
    }

    private ConnectorMessageAttachment persistPdfTrustToken(
        ConnectorContainer container,
        ConnectorBusinessMessage message)
        throws IOException {
        return persistAttachment(
            newAttachmentMetadata(
                ConnectorContainerFileDefinitions.TOKEN_PDF.name(),
                MediaType.APPLICATION_PDF_VALUE,
                "PDF Trust OK Token",
                ConnectorAttachmentType.PDF_TOKEN,
                documentSize(container.tokenPDF())
            ),
            container.tokenPDF().openStream(),
            message.identifier()
        );
    }

    private ConnectorMessageBusinessDocument persistBusinessDocument(
        ConnectorContainer container,
        ConnectorBusinessMessage message)
        throws IOException {
        var businessDssDocument = container.businessContent().getDocument();
        if (businessDssDocument == null) {
            return null;
        }

        var businessDocumentAttachment = newAttachmentMetadata(
            container.token().getDocument().getFilename(),
            mimeTypeString(businessDssDocument),
            "Business document",
            ConnectorAttachmentType.BUSINESS_DOCUMENT,
            documentSize(businessDssDocument)
        );
        persistAttachment(
            businessDocumentAttachment,
            businessDssDocument.openStream(),
            message.identifier()
        );

        var businessDocumentBuilder = ConnectorMessageBusinessDocument
            .builder()
            .content(businessDocumentAttachment);

        var detachedSignature = persistDetachedSignature(
            container.businessContent().getDetachedSignature(),
            message
        );

        if (detachedSignature != null) {
            businessDocumentBuilder.detachedSignature(detachedSignature);
            businessDocumentBuilder.aesType(ConnectorBusinessDocumentAESType.SIGNATURE_BASED);
        }

        var businessDocument = businessDocumentBuilder.build();
        var businessContent = this.businessContentRepository.assignBusinessDocument(
            message.businessContent().uuid(),
            businessDocument
        );

        return businessContent.businessDocument();
    }

    private ConnectorMessageAttachment persistDetachedSignature(
        DSSDocument detachedDssSignature,
        ConnectorBusinessMessage message)
        throws IOException {
        if (detachedDssSignature == null) {
            return null;
        }

        var mimeType = DetachedSignatureMimeType.fromMimeType(mimeTypeString(detachedDssSignature));

        byte[] signatureBytes = DSSUtils.toByteArray(detachedDssSignature.openStream());

        return persistAttachment(
            newAttachmentMetadata(
                detachedDssSignature.getName(),
                mimeType.getMimeType(),
                "Detached signature",
                ConnectorAttachmentType.DETACHED_SIGNATURE,
                signatureBytes.length
            ),
            new ByteArrayInputStream(signatureBytes),
            message.identifier()
        );
    }

    private List<ConnectorMessageAttachment> persistAdditionalAttachments(
        ConnectorContainer container,
        ConnectorBusinessMessage message)
        throws IOException {
        List<ConnectorMessageAttachment> attachments = new ArrayList<>();
        for (var attachment : container.businessContent().getAttachments()) {
            var connectorMessageAttachment = persistAttachment(
                newAttachmentMetadata(
                    attachment.getName(),
                    mimeTypeString(attachment),
                    "Attachment",
                    ConnectorAttachmentType.ATTACHMENT,
                    documentSize(attachment)
                ),
                attachment.openStream(),
                message.identifier()
            );
            attachments.add(connectorMessageAttachment);
        }

        return attachments;
    }

    private ConnectorMessageAttachment newAttachmentMetadata(
        String name,
        String contentType,
        String description,
        ConnectorAttachmentType type,
        int size) {
        var identifier = UUID.randomUUID() + "_" + name;
        return ConnectorMessageAttachment.builder()
                                         .identifier(identifier)
                                         .name(name)
                                         .contentType(contentType)
                                         .size(size)
                                         .description(description)
                                         .storage(ConnectorAttachmentStorage.S3_BUCKET)
                                         .type(type)
                                         .build();
    }

    /**
     * Uploads to storage first, then writes the DB record. This ordering prevents orphaned metadata
     * rows if the upload fails.
     */
    private ConnectorMessageAttachment persistAttachment(
        ConnectorMessageAttachment attachment,
        InputStream inputStream,
        String messageIdentifier) throws IOException {
        // TODO: use a streaming save for large files rather than readAllBytes()
        byte[] content;

        try (inputStream) {
            content = inputStream.readAllBytes();
        }

        fileStorageProvider.save(attachment, content);
        var persisted = attachmentRepository.save(attachment);
        attachmentRepository.attachToMessage(attachment.identifier(), messageIdentifier);

        return persisted;
    }

    /**
     * Returns the byte length of a DSS document. Only {@link InMemoryDocument} is supported until
     * large-file handling is implemented.
     */
    private int documentSize(DSSDocument document) {
        if (!(document instanceof InMemoryDocument inMemory)) {
            throw new ConnectorContainerException(
                "Document size is not yet supported for " + document.getClass().getSimpleName()
                    + " — only InMemoryDocument is currently handled");
        }
        // TODO: adapt to large-file support using an appropriate DSSDocument implementation
        return inMemory.getBytes().length;
    }

    private String mimeTypeString(DSSDocument document) {
        var mimeType = document.getMimeType();
        return mimeType == null ? null : mimeType.getMimeTypeString();
    }

    private void checkContainer(ConnectorContainer container, ConnectorBusinessMessage message) {
        checkers.forEach(checker -> checker.check(container));
        signatureChecker.check(container, message.businessDomainIdentifier());
    }
}
