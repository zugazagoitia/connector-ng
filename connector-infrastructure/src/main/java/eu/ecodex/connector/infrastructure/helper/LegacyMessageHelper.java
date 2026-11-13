/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.helper;

import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageAS4Properties;
import eu.ecodex.connector.domain.model.message.ConnectorMessageError;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessDocument;
import eu.ecodex.connector.domain.model.message.content.DetachedSignatureMimeType;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import eu.ecodex.connector.domain.transition.DomibusConnectorActionType;
import eu.ecodex.connector.domain.transition.DomibusConnectorConfirmationType;
import eu.ecodex.connector.domain.transition.DomibusConnectorDetachedSignatureMimeType;
import eu.ecodex.connector.domain.transition.DomibusConnectorDetachedSignatureType;
import eu.ecodex.connector.domain.transition.DomibusConnectorDocumentAESType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageAttachmentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageConfirmationType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageContentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageDetailsType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageDocumentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageErrorType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageType;
import eu.ecodex.connector.domain.transition.DomibusConnectorPartyType;
import eu.ecodex.connector.domain.transition.DomibusConnectorServiceType;
import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import org.springframework.stereotype.Component;

/**
 * Helper class for converting {@link ConnectorBusinessMessage} instances to legacy message types.
 */
@Component
public class LegacyMessageHelper {
    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorFileStorageProvider fileStorageProvider;

    public LegacyMessageHelper(
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorFileStorageProvider fileStorageProvider) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageProvider = fileStorageProvider;
    }

    /**
     * Converts a {@link ConnectorBusinessMessage} into a {@link DomibusConnectorMessageType}.
     *
     * @param message the {@link ConnectorBusinessMessage} instance to be converted
     *
     * @return a {@link DomibusConnectorMessageType} instance containing the converted data from the
     *     input message
     */
    public DomibusConnectorMessageType convertMessage(ConnectorMessage message) {
        var details = convertDetails(message);

        var messageType = new DomibusConnectorMessageType();
        messageType.setMessageDetails(details);

        if (message instanceof ConnectorBusinessMessage businessMessage) {
            messageType.setMessageContent(toContent(businessMessage.businessContent()));
            messageType.getMessageErrors().addAll(toErrors(businessMessage.errors()));
            messageType.getMessageAttachments().addAll(toAttachments(message.identifier()));
        }

        messageType.getMessageConfirmations().addAll(
            toConfirmations(message.transportedEvidences())
        );

        return messageType;
    }

    private DomibusConnectorMessageDetailsType convertDetails(ConnectorMessage message) {
        var as4Properties = message.as4Properties();

        var details = new DomibusConnectorMessageDetailsType();
        details.setBackendMessageId(message.backendMessageIdentifier());
        details.setConversationId(as4Properties.conversationIdentifier());
        details.setEbmsMessageId(as4Properties.ebmsMessageIdentifier());
        details.setRefToMessageId(as4Properties.referenceToIdentifier());
        details.setOriginalSender(as4Properties.originalSender());
        details.setFinalRecipient(as4Properties.finalRecipient());
        details.setService(toService(as4Properties));
        details.setAction(toAction(as4Properties));
        details.setFromParty(getParty(as4Properties.fromParty()));
        details.setToParty(getParty(as4Properties.toParty()));

        return details;
    }

    private DomibusConnectorServiceType toService(ConnectorMessageAS4Properties as4Properties) {
        var service = new DomibusConnectorServiceType();
        service.setService(as4Properties.service().name());
        service.setServiceType(as4Properties.service().type());

        return service;
    }

    private DomibusConnectorActionType toAction(ConnectorMessageAS4Properties as4Properties) {
        if (as4Properties.action().name() == null) {
            throw new IllegalStateException();
        }

        var action = new DomibusConnectorActionType();
        action.setAction(as4Properties.action().name());

        return action;
    }

    private DomibusConnectorPartyType getParty(ConnectorProcessingModeParty party) {
        if (party == null) {
            throw new IllegalStateException("Party cannot be null");
        }

        var part = new DomibusConnectorPartyType();
        part.setPartyId(party.identifier());
        part.setPartyIdType(party.identifierType());
        part.setRole(party.role());

        return part;
    }

    private DomibusConnectorMessageContentType toContent(ConnectorMessageBusinessContent content) {
        if (content == null) {
            return null;
        }

        var data = this.fileStorageProvider.findByIdentifier(content.xmlContent().identifier());
        var contentType = new DomibusConnectorMessageContentType();
        contentType.setXmlContent(
            new StreamSource(
                new ByteArrayInputStream(Arrays.copyOf(data, data.length))
            )
        );
        contentType.setDocument(toDocument(content.businessDocument()));

        return contentType;
    }

    private DomibusConnectorMessageDocumentType toDocument(
        ConnectorMessageBusinessDocument document) {
        if (document == null) {
            return null;
        }

        var data = this.fileStorageProvider.findByIdentifier(document.content().identifier());

        var documentType = new DomibusConnectorMessageDocumentType();
        documentType.setDocumentName(document.content().name());
        documentType.setDocument(
            new DataHandler(
                new ByteArrayDataSource(
                    Arrays.copyOf(data, data.length),
                    document.content().contentType()
                )
            )
        );

        if (document.aesType() != null) {
            documentType.setAesType(
                DomibusConnectorDocumentAESType.fromValue(document.aesType().name())
            );
        }

        var detachedSignature = document.detachedSignature();

        if (detachedSignature != null) {
            var detachedSignatureType = new DomibusConnectorDetachedSignatureType();
            var mimeType = DomibusConnectorDetachedSignatureMimeType.fromValue(
                DetachedSignatureMimeType.fromMimeType(detachedSignature.contentType()).toString()
            );
            detachedSignatureType.setMimeType(mimeType);
            detachedSignatureType.setDetachedSignatureName(document.detachedSignature().name());

            var content = this.fileStorageProvider.findByIdentifier(detachedSignature.identifier());

            detachedSignatureType.setDetachedSignature(content);
            documentType.setDetachedSignature(detachedSignatureType);
        }

        return documentType;
    }

    private List<DomibusConnectorMessageAttachmentType> toAttachments(String messageIdentifier) {
        var attachments = this.attachmentRepository.findByMessageIdentifierAndTypes(
            messageIdentifier,
            List.of(
                ConnectorAttachmentType.ATTACHMENT,
                ConnectorAttachmentType.PDF_TOKEN,
                ConnectorAttachmentType.XML_TOKEN
            )
        );

        return attachments.stream().map(attachment -> {
            var data = this.fileStorageProvider.findByIdentifier(attachment.identifier());
            var attachmentType = new DomibusConnectorMessageAttachmentType();
            attachmentType.setIdentifier(attachment.identifier());
            attachmentType.setName(attachment.name());
            attachmentType.setMimeType(attachment.contentType());
            attachmentType.setDescription(attachment.description());
            attachmentType.setAttachment(
                new DataHandler(
                    new ByteArrayDataSource(
                        Arrays.copyOf(data, data.length), attachment.contentType()
                    )
                )
            );

            return attachmentType;
        }).toList();
    }

    private List<DomibusConnectorMessageConfirmationType> toConfirmations(
        List<ConnectorMessageEvidence> transportedEvidences) {

        if (transportedEvidences == null || transportedEvidences.isEmpty()) {
            return new ArrayList<>();
        }

        return transportedEvidences.stream().map(evidence -> {
            byte[] content = evidence.content();

            if (content == null) {
                throw new IllegalStateException(
                    "Evidence content is null for evidence " + evidence.type()
                );
            }

            var confirmation = new DomibusConnectorMessageConfirmationType();
            confirmation.setConfirmationType(
                DomibusConnectorConfirmationType.fromValue(evidence.type().name())
            );
            confirmation.setConfirmation(
                new StreamSource(
                    new ByteArrayInputStream(Arrays.copyOf(
                        evidence.content(),
                        evidence.content().length
                    ))
                )
            );

            return confirmation;
        }).toList();
    }

    private List<DomibusConnectorMessageErrorType> toErrors(List<ConnectorMessageError> errors) {
        if (errors == null || errors.isEmpty()) {
            return new ArrayList<>();
        }

        return errors.stream().map(error -> {
            var errorType = new DomibusConnectorMessageErrorType();
            errorType.setErrorSource(error.source());
            errorType.setErrorMessage(error.label());
            errorType.setErrorDetails(error.details());

            return errorType;
        }).toList();
    }
}
