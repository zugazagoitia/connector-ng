/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.security;

import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.ConnectorSecurityToolkit;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.infrastructure.adapter.out.security.container.ConnectorContainerBuilder;
import eu.ecodex.connector.infrastructure.adapter.out.security.container.ConnectorContainerValidator;
import eu.ecodex.connector.infrastructure.adapter.out.security.exception.ConnectorContainerException;
import eu.ecodex.connector.infrastructure.adapter.out.security.model.container.ConnectorContainer;
import eu.ecodex.connector.infrastructure.helper.ConnectorTempFilesHelper;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Default Implementation of the {@link ConnectorSecurityToolkit}.
 */
@Slf4j
@Component
public class ConnectorDssSecurityToolkit implements ConnectorSecurityToolkit {
    private final ConnectorContainerBuilder containerBuilder;
    private final ConnectorContainerValidator containerValidator;
    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorFileStorageProvider fileStorageProvider;
    private final ConnectorTempFilesHelper tempFilesHelper;

    /**
     * Creates the security toolkit, which builds and validates the ASiC containers of the messages
     * using the DSS library.
     *
     * @param containerBuilder     the builder used to create the ASiC containers of outbound
     *                             messages (must not be null)
     * @param containerValidator   the validator used to validate the ASiC containers of inbound
     *                             messages (must not be null)
     * @param attachmentRepository the repository used to find and save the attachments of the
     *                             messages (must not be null)
     * @param fileStorageProvider  the provider used to read and write the content of the
     *                             attachments in the file storage (must not be null)
     * @param tempFilesHelper      the helper used to create the temporary files needed while
     *                             building or validating containers (must not be null)
     */

    public ConnectorDssSecurityToolkit(
        ConnectorContainerBuilder containerBuilder,
        ConnectorContainerValidator containerValidator,
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorFileStorageProvider fileStorageProvider,
        ConnectorTempFilesHelper tempFilesHelper) {
        this.containerBuilder = containerBuilder;
        this.containerValidator = containerValidator;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageProvider = fileStorageProvider;
        this.tempFilesHelper = tempFilesHelper;
    }

    @Override
    public ConnectorBusinessMessage validateMessage(@NonNull ConnectorBusinessMessage message) {
        log.debug("Validating message [{}] ASIC-S container", message.identifier());
        return this.containerValidator.validate(message);
    }

    @Override
    public ConnectorBusinessMessage buildContainer(@NonNull ConnectorBusinessMessage message) {
        var messageIdentifier = message.identifier();

        try {
            this.validateUniqueAttachmentNames(messageIdentifier);

            var container = this.containerBuilder.createAsicContainer(message);
            this.saveContainer(messageIdentifier, container);
            this.saveXmlToken(messageIdentifier, container);

            return message;
        } catch (ConnectorContainerException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build ASIC-S container for the message: [{}]", messageIdentifier);
            throw new ConnectorContainerException(
                "Failed to build ASIC-S container for the message: " + messageIdentifier, e
            );
        }
    }

    private void validateUniqueAttachmentNames(String messageIdentifier) {
        log.debug("Validating unique attachment names for message: [{}]", messageIdentifier);

        var attachments = this.attachmentRepository.findByMessageIdentifierAndTypes(
            messageIdentifier, List.of(ConnectorAttachmentType.ATTACHMENT)
        );

        var seenNames = new HashSet<String>();

        for (var attachment : attachments) {
            var name = attachment.name();

            if (!seenNames.add(name)) {
                throw new ConnectorContainerException(
                    String.format(
                        "Duplicate attachment name [%s] detected for message [%s]",
                        name, messageIdentifier
                    )
                );
            }
        }
    }

    private void saveContainer(String messageIdentifier, ConnectorContainer container)
        throws Exception {
        log.debug("Saving ASIC-S container for message: [{}]", messageIdentifier);

        var attachment = ConnectorMessageAttachment
            .builder()
            .identifier(String.format("message-%s-asics", messageIdentifier))
            .name("container-signed-xades-baseline-b.scs")
            .contentType("application/vnd.etsi.asic-s+zip")
            .size(getDssFileSize(container.asicDocument()))
            .description(String.format("ASIC-S container for message: %s", messageIdentifier))
            .storage(ConnectorAttachmentStorage.S3_BUCKET)
            .type(ConnectorAttachmentType.ASICS)
            .build();

        saveAttachmentFile(
            attachment, messageIdentifier,
            tempPath -> container.asicDocument().save(tempPath.toString()),
            "ASIC-S container"
        );
    }

    private void saveXmlToken(String messageIdentifier, ConnectorContainer container)
        throws Exception {
        log.debug("Saving XML TrustOK Token for message: [{}]", messageIdentifier);

        var attachment = ConnectorMessageAttachment
            .builder()
            .identifier(String.format("message-%s-trustok-token.xml", messageIdentifier))
            .name("Token.xml")
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .size(getDssFileSize(container.tokenXML()))
            .description(String.format("XML TrustOK Token for message: %s", messageIdentifier))
            .storage(ConnectorAttachmentStorage.S3_BUCKET)
            .type(ConnectorAttachmentType.XML_TOKEN)
            .build();

        saveAttachmentFile(
            attachment, messageIdentifier,
            tempPath -> container.tokenXML().save(tempPath.toString()),
            "XML TrustOK Token"
        );
    }

    /**
     * Saves an attachment by: persisting metadata, writing to a temp file, uploading to storage,
     * then deleting the temp file. Temp file is always cleaned up, even on failure.
     */
    private void saveAttachmentFile(
        ConnectorMessageAttachment attachment,
        String messageIdentifier,
        SaveOperation saveOperation,
        String description) throws IOException {
        var savedAttachment = attachmentRepository.save(attachment);
        attachmentRepository.attachToMessage(savedAttachment.identifier(), messageIdentifier);

        var tempPath = tempFilesHelper.create("connector", savedAttachment.identifier());
        try {
            saveOperation.save(tempPath);
            fileStorageProvider.save(savedAttachment, tempPath);
            Files.delete(tempPath);
        } catch (Exception e) {
            Files.deleteIfExists(tempPath);
            log.error("Failed to save {} for message [{}]", description, messageIdentifier, e);
            throw new ConnectorContainerException(e);
        }
    }

    private int getDssFileSize(DSSDocument dssDocument) {
        try {
            return ((InMemoryDocument) dssDocument).getBytes().length;
        } catch (Exception e) {
            return -1;
        }
    }

    @FunctionalInterface
    private interface SaveOperation {
        void save(Path path) throws Exception;
    }
}
