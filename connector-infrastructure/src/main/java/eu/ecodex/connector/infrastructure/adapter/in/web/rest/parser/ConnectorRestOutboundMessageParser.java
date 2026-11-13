/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.parser;

import eu.ecodex.connector.application.port.api.attachment.ConnectorUploadAttachments;
import eu.ecodex.connector.application.port.api.attachment.FileUploadCommand;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomain;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessDocument;
import eu.ecodex.connector.domain.model.pmode.ConnectorPartyRoleType;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.exception.ConnectorAttachmentUploadException;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.message.outbound.ConnectorOutboundMessageBusinessContent;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.message.outbound.ConnectorOutboundMessageDetachedSignature;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.message.outbound.ConnectorOutboundMessageParty;
import eu.ecodex.connector.infrastructure.helper.ConnectorTempFilesHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Parses outbound message data received through the REST API and converts it into connector domain
 * objects.
 */
@Component
public class ConnectorRestOutboundMessageParser {
    private final ConnectorUploadAttachments uploadAttachmentsService;
    private final ConnectorTempFilesHelper tempFilesHelper;

    public ConnectorRestOutboundMessageParser(
        ConnectorUploadAttachments uploadAttachmentsService,
        ConnectorTempFilesHelper tempFilesHelper) {
        this.uploadAttachmentsService = uploadAttachmentsService;
        this.tempFilesHelper = tempFilesHelper;
    }

    /**
     * Resolves a business domain identifier from the provided message lane identifier.
     *
     * <p>If the identifier is {@code null}, the default business domain identifier
     * is returned.</p>
     *
     * @param identifier the message lane identifier, or {@code null} to use the default
     *
     * @return the resolved business domain identifier
     */
    public ConnectorBusinessDomainIdentifier resolveBusinessDomainIdentifier(String identifier) {
        if (identifier == null) {
            return ConnectorBusinessDomain.DEFAULT_BUSINESS_DOMAIN_ID;
        }

        return ConnectorBusinessDomainIdentifier
            .builder()
            .messageLaneIdentifier(identifier)
            .build();
    }

    /**
     * Converts business content into connector message business content.
     *
     * @param businessContent the representation of the business content
     *
     * @return the corresponding connector message business content
     *
     * @throws IOException if an attachment cannot be read or uploaded
     */
    public ConnectorMessageBusinessContent toBusinessContent(
        ConnectorOutboundMessageBusinessContent businessContent) throws IOException {
        var businessDocumentRequest = businessContent.businessDocument();
        var businessDocument = ConnectorMessageBusinessDocument
            .builder()
            .content(toAttachment(
                businessContent.businessDocument().document(),
                "Business document"
            ))
            .detachedSignature(toDetachedSignature(businessDocumentRequest.detachedSignature()))
            .aesType(businessDocumentRequest.aesType())
            .build();

        return ConnectorMessageBusinessContent
            .builder()
            .xmlContent(toAttachment(businessContent.xmlContent(), "Business content"))
            .businessDocument(businessDocument)
            .build();
    }

    private ConnectorMessageAttachment toDetachedSignature(
        ConnectorOutboundMessageDetachedSignature detachedSignature) throws IOException {
        if (detachedSignature == null || detachedSignature.signature() == null) {
            return null;
        }

        return toAttachment(detachedSignature.signature(), "Business document detached signature");
    }

    /**
     * Uploads a multipart file and converts it into a connector message attachment.
     *
     * @param file        the multipart file to upload
     * @param description the attachment description
     *
     * @return the resulting connector message attachment
     *
     * @throws IOException                        if the temporary file cannot be created or
     *                                            deleted
     * @throws ConnectorAttachmentUploadException if the attachment upload fails
     */
    public ConnectorMessageAttachment toAttachment(MultipartFile file, String description)
        throws IOException {
        var filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        var tempLocation = tempFilesHelper.create("upload-", filename);

        try {
            file.transferTo(tempLocation);
            var uploadCommand = FileUploadCommand
                .builder()
                .contentType(Objects.requireNonNull(file.getContentType()))
                .filename(filename)
                .tempFileLocation(tempLocation)
                .size(file.getSize())
                .description(description)
                .build();

            return uploadAttachmentsService.execute(List.of(uploadCommand)).getFirst();
        } catch (Exception e) {
            throw new ConnectorAttachmentUploadException(
                "Failed to upload attachment: " + file.getName(), e);
        } finally {
            // Always runs, covering both success and failure paths
            Files.deleteIfExists(tempLocation);
        }
    }

    private ConnectorMessageAttachment toAttachment(String identifier) {
        return ConnectorMessageAttachment
            .builder()
            .identifier(identifier)
            .build();
    }

    /**
     * Converts a list of attachment identifiers into connector message attachments.
     *
     * @param identifiers the attachment identifiers, or {@code null}
     *
     * @return the corresponding attachments, or an empty list if the identifiers are {@code null}
     */
    public List<ConnectorMessageAttachment> toAttachments(List<String> identifiers) {
        if (identifiers == null) {
            return new ArrayList<>();
        }

        return identifiers.stream()
                          .map(this::toAttachment)
                          .toList();
    }

    /**
     * Converts a message party into a connector party.
     *
     * @param party    the representation of the party
     * @param roleType the connector role type assigned to the party
     *
     * @return the corresponding connector party
     */
    public ConnectorProcessingModeParty toParty(
        ConnectorOutboundMessageParty party,
        ConnectorPartyRoleType roleType) {
        return ConnectorProcessingModeParty
            .builder()
            .identifier(party.identifier())
            .identifierType(party.identifierType())
            .role(party.role())
            .roleType(roleType)
            .build();
    }
}
