/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.soap.controller;

import eu.ecodex.connector.application.port.api.attachment.ConnectorUploadAttachments;
import eu.ecodex.connector.application.port.api.attachment.FileUploadCommand;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundBusinessMessageReceiver;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundEvidenceMessageReceiver;
import eu.ecodex.connector.application.port.api.transport.ConnectorAckMessageTransportStep;
import eu.ecodex.connector.application.port.api.transport.ConnectorListPendingTransportSteps;
import eu.ecodex.connector.application.port.api.transport.ConnectorListPendingTransportStepsIds;
import eu.ecodex.connector.application.port.api.transport.ConnectorRetrieveMessageByTransportId;
import eu.ecodex.connector.application.port.api.transport.ConnectorSetMessagesTransportStepToPull;
import eu.ecodex.connector.application.port.api.transport.command.UpdateMessageTransportCommand;
import eu.ecodex.connector.domain.model.message.ConnectorMessageError;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.transition.DomibsConnectorAcknowledgementType;
import eu.ecodex.connector.domain.transition.DomibusConnectorBackendWebService;
import eu.ecodex.connector.domain.transition.DomibusConnectorDetachedSignatureType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageAttachmentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageContentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageDocumentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageErrorType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageResponseType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessagesType;
import eu.ecodex.connector.domain.transition.EmptyRequestType;
import eu.ecodex.connector.domain.transition.GetMessageByIdRequest;
import eu.ecodex.connector.domain.transition.ListPendingMessageIdsResponse;
import eu.ecodex.connector.infrastructure.adapter.in.web.ConnectorBackendClientVerifier;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.exception.ConnectorInternalServerException;
import eu.ecodex.connector.infrastructure.adapter.in.web.soap.helper.ConnectorSOAPMessageAttachmentHelpers;
import eu.ecodex.connector.infrastructure.adapter.in.web.soap.helper.MessageHelpers;
import eu.ecodex.connector.infrastructure.adapter.in.web.soap.interceptor.ProcessMessageAfterDownload;
import eu.ecodex.connector.infrastructure.adapter.in.web.soap.interceptor.ProcessMessagesAfterDownload;
import eu.ecodex.connector.infrastructure.helper.LegacyMessageHelper;
import eu.europa.esig.dss.enumerations.MimeType;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.soap.MTOM;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Spring-managed web service controller responsible for handling backend message submissions for
 * the connector.
 *
 * <p>This component implements {@link DomibusConnectorBackendWebService}
 * and acts as the integration entry point between backend systems and the connector domain.
 */
@Slf4j
@MTOM
@Component
public class ConnectorBackendWebServiceController implements DomibusConnectorBackendWebService {
    private final ConnectorOutboundBusinessMessageReceiver businessMessageReceiver;
    private final ConnectorOutboundEvidenceMessageReceiver outboundEvidenceMessageProcessor;
    private final ConnectorListPendingTransportStepsIds listPendingMessageIds;
    private final ConnectorListPendingTransportSteps listPendingMessages;
    private final ConnectorRetrieveMessageByTransportId retrieveMessageByTransportId;
    private final ConnectorUploadAttachments uploadAttachments;
    private final ConnectorSetMessagesTransportStepToPull changePendingMessagesStatus;
    private final ConnectorAckMessageTransportStep ackMessageTransportStep;
    private final ConnectorBackendClientVerifier backendClientVerifier;
    private final LegacyMessageHelper legacyMessageHelper;
    private final ConnectorSOAPMessageAttachmentHelpers attachmentHelpers;
    private final Tika tika;
    private WebServiceContext webServiceContext;

    /**
     * Creates the controller of the backend web service, which lets a backend system exchange
     * messages with the connector: submit outbound business and evidence messages, upload
     * attachments, retrieve the pending messages, and acknowledge their transport.
     *
     * @param businessMessageReceiver          the use case that receives outbound business messages
     *                                         and prepares them for dispatch (must not be null)
     * @param outboundEvidenceMessageProcessor the use case that receives the outbound evidence
     *                                         trigger messages submitted by the backend (must not
     *                                         be null)
     * @param listPendingMessageIds            the use case that lists the identifiers of the
     *                                         pending transport steps of a backend (must not be
     *                                         null)
     * @param listPendingMessages              the use case that lists the pending transport steps
     *                                         of a backend, each carrying the message to retrieve
     *                                         (must not be null)
     * @param retrieveMessageByTransportId     the use case that retrieves a message by its
     *                                         transport identifier (must not be null)
     * @param uploadAttachments                the use case that uploads message attachments (must
     *                                         not be null)
     * @param changePendingMessagesStatus      the use case that marks the pending messages of a
     *                                         backend as ready to be downloaded (must not be null)
     * @param ackMessageTransportStep          the use case that updates the transport status of a
     *                                         message when the backend acknowledges it (must not be
     *                                         null)
     * @param backendClientVerifier            the verifier that checks that a request comes from a
     *                                         known backend client (must not be null)
     * @param legacyMessageHelper              the helper that converts messages to and from the
     *                                         legacy message format (must not be null)
     * @param attachmentHelpers                the helpers used to process the attachments of the
     *                                         requests (must not be null)
     */
    public ConnectorBackendWebServiceController(
        ConnectorOutboundBusinessMessageReceiver businessMessageReceiver,
        ConnectorOutboundEvidenceMessageReceiver outboundEvidenceMessageProcessor,
        ConnectorListPendingTransportStepsIds listPendingMessageIds,
        ConnectorListPendingTransportSteps listPendingMessages,
        ConnectorRetrieveMessageByTransportId retrieveMessageByTransportId,
        ConnectorUploadAttachments uploadAttachments,
        ConnectorSetMessagesTransportStepToPull changePendingMessagesStatus,
        ConnectorAckMessageTransportStep ackMessageTransportStep,
        ConnectorBackendClientVerifier backendClientVerifier,
        LegacyMessageHelper legacyMessageHelper,
        ConnectorSOAPMessageAttachmentHelpers attachmentHelpers) {
        this.businessMessageReceiver = businessMessageReceiver;
        this.outboundEvidenceMessageProcessor = outboundEvidenceMessageProcessor;
        this.listPendingMessageIds = listPendingMessageIds;
        this.listPendingMessages = listPendingMessages;
        this.retrieveMessageByTransportId = retrieveMessageByTransportId;
        this.uploadAttachments = uploadAttachments;
        this.changePendingMessagesStatus = changePendingMessagesStatus;
        this.ackMessageTransportStep = ackMessageTransportStep;
        this.backendClientVerifier = backendClientVerifier;
        this.legacyMessageHelper = legacyMessageHelper;
        this.attachmentHelpers = attachmentHelpers;
        this.tika = new Tika();
    }

    @Resource
    public void setWsContext(WebServiceContext webServiceContext) {
        this.webServiceContext = webServiceContext;
    }

    @Override
    public EmptyRequestType acknowledgeMessage(DomibusConnectorMessageResponseType responseType) {
        var backendClientName = getBackendClientName();
        var commandBuilder = UpdateMessageTransportCommand
            .builder()
            .linkPartnerName(backendClientName)
            .remoteMessageIdentifier(responseType.getAssignedMessageId());
        if (responseType.isResult()) {
            commandBuilder.status(ConnectorMessageTransportStatus.DELIVERED);
            commandBuilder.errors(null);
        } else {
            commandBuilder.status(ConnectorMessageTransportStatus.FAILED);
            commandBuilder.errors(toDomainErrors(responseType.getMessageErrors()));
        }

        ackMessageTransportStep.execute(
            responseType.getResponseForMessageId(),
            commandBuilder.build()
        );

        return new EmptyRequestType();
    }

    @Override
    public ListPendingMessageIdsResponse listPendingMessageIds(
        EmptyRequestType listPendingMessageIdsRequest) {
        try {
            var backendClientName = getBackendClientName();
            var response = new ListPendingMessageIdsResponse();
            response.getMessageTransportIds().addAll(
                listPendingMessageIds.execute(backendClientName)
            );

            return response;
        } catch (Exception e) {
            log.error("Error listing pending messages from the connector via SOAP endpoint", e);

            throw e;
        }
    }

    @Override
    public DomibusConnectorMessageType getMessageById(GetMessageByIdRequest getMessageByIdRequest) {
        var backendClientName = getBackendClientName();
        var transportIdentifier = getMessageByIdRequest.getMessageTransportId();
        var message = retrieveMessageByTransportId.execute(transportIdentifier, backendClientName);

        // add post invoke message processor
        var messageContext = webServiceContext.getMessageContext();
        var wrappedMessageContext = (WrappedMessageContext) messageContext;
        var interceptor = new ProcessMessageAfterDownload(
            message,
            backendClientName,
            ackMessageTransportStep

        );
        wrappedMessageContext.getWrappedMessage().getInterceptorChain().add(interceptor);

        return this.legacyMessageHelper.convertMessage(message);
    }

    @Override
    public DomibusConnectorMessagesType requestMessages(EmptyRequestType requestMessagesRequest) {
        try {
            var backendClientName = getBackendClientName();
            var messages = this.listPendingMessages.execute(backendClientName);
            var legacyMessages = messages
                .stream()
                .map(step -> this.legacyMessageHelper.convertMessage(step.transportedMessage()))
                .toList();

            var response = new DomibusConnectorMessagesType();
            response.getMessages().addAll(legacyMessages);

            // add post invoke message processor
            var messageContext = webServiceContext.getMessageContext();
            var wrappedMessageContext = (WrappedMessageContext) messageContext;
            var interceptor = new ProcessMessagesAfterDownload(
                backendClientName,
                changePendingMessagesStatus

            );
            wrappedMessageContext.getWrappedMessage().getInterceptorChain().add(interceptor);

            return response;
        } catch (Exception e) {
            log.error("Error requesting messages from the connector via SOAP endpoint", e);

            throw e;
        }
    }

    @Override
    public DomibsConnectorAcknowledgementType submitMessage(
        DomibusConnectorMessageType submitMessageRequest) {
        var answer = new DomibsConnectorAcknowledgementType();

        try {
            var backendClientName = getBackendClientName();

            if (MessageHelpers.isEvidenceTriggerRequest(submitMessageRequest)) {
                var evidenceMessageCommand = MessageHelpers.toEvidenceTriggerCommand(
                    submitMessageRequest,
                    backendClientName
                );
                var createdMessage = this.outboundEvidenceMessageProcessor.execute(
                    evidenceMessageCommand
                );
                answer.setMessageId(createdMessage.identifier());
            } else {
                var attachmentIdentifiers = persistAttachments(
                    submitMessageRequest.getMessageAttachments()
                );
                var businessContent = submitMessageRequest.getMessageContent();
                var businessDocumentAttachmentIdentifier = persistBusinessDocument(
                    businessContent.getDocument()
                );
                var detachedSignature = businessContent.getDocument().getDetachedSignature();

                String businessDocDetachedSigAttachmentId = null;

                if (detachedSignature != null) {
                    businessDocDetachedSigAttachmentId =
                        persistBusinessDocumentDetachedSignature(detachedSignature);
                }

                var businessContentAttachmentIdentifier = persistBusinessContent(businessContent);
                var parsedMessage = MessageHelpers.toBusinessCommand(
                    submitMessageRequest,
                    attachmentIdentifiers,
                    businessContentAttachmentIdentifier,
                    businessDocumentAttachmentIdentifier,
                    businessDocDetachedSigAttachmentId,
                    backendClientName
                );

                var createdMessage = this.businessMessageReceiver.execute(parsedMessage);
                answer.setMessageId(createdMessage.identifier());
            }

            answer.setResult(true);
        } catch (Exception e) {
            log.error("Error submitting message to the connector via SOAP endpoint", e);
            answer.setResult(false);
            answer.setMessageId(submitMessageRequest.getMessageDetails().getBackendMessageId());
            answer.setResultMessage(e.getMessage());
        }

        return answer;
    }

    private List<String> persistAttachments(
        List<DomibusConnectorMessageAttachmentType> attachments) {
        if (attachments.isEmpty()) {
            return new ArrayList<>();
        }

        var fileUploadCommands = toFileUploadCommands(attachments);

        try {
            return uploadAttachments.execute(fileUploadCommands)
                                    .stream()
                                    .map(ConnectorMessageAttachment::identifier)
                                    .toList();
        } finally {
            fileUploadCommands.forEach(FileUploadCommand::cleanup);
        }
    }

    private String persistBusinessContent(DomibusConnectorMessageContentType businessContent) {
        var fileUploadCommand = toFileUploadCommand(businessContent);

        try {
            return uploadAttachments.execute(List.of(fileUploadCommand))
                                    .getFirst()
                                    .identifier();
        } finally {
            fileUploadCommand.cleanup();
        }
    }

    private String persistBusinessDocument(DomibusConnectorMessageDocumentType pdfBusinessContent) {
        var fileUploadCommand = toFileUploadCommand(pdfBusinessContent);

        try {
            return uploadAttachments.execute(List.of(fileUploadCommand))
                                    .getFirst()
                                    .identifier();
        } finally {
            fileUploadCommand.cleanup();
        }
    }

    private String persistBusinessDocumentDetachedSignature(
        DomibusConnectorDetachedSignatureType detachedSignature) {
        var fileUploadCommand = toFileUploadCommand(detachedSignature);

        try {
            return uploadAttachments.execute(List.of(fileUploadCommand))
                                    .getFirst()
                                    .identifier();
        } finally {
            fileUploadCommand.cleanup();
        }
    }

    private FileUploadCommand toFileUploadCommand(
        DomibusConnectorMessageDocumentType pdfBusinessContent) {
        try {
            var tempFile = attachmentHelpers.dataHandlerToTempFile(
                pdfBusinessContent.getDocument());
            var file = tempFile.toFile();
            var contentType = getContentType(tempFile);

            return FileUploadCommand
                .builder()
                .filename(StringUtils.cleanPath(pdfBusinessContent.getDocumentName()))
                .contentType(contentType)
                .size(file.length())
                .tempFileLocation(tempFile)
                .description("Registered business document")
                .build();
        } catch (Exception e) {
            throw new ConnectorInternalServerException(e.getMessage());
        }
    }

    private FileUploadCommand toFileUploadCommand(
        DomibusConnectorDetachedSignatureType detachedSignature) {
        try {
            var contentType = MimeType.fromMimeTypeString(
                detachedSignature.getMimeType().toString()
            ).getMimeTypeString();

            var tempFile = attachmentHelpers.bytesToTempFile(
                detachedSignature.getDetachedSignature(),
                detachedSignature.getDetachedSignatureName()
            );
            var file = tempFile.toFile();

            return FileUploadCommand
                .builder()
                .filename(StringUtils.cleanPath(detachedSignature.getDetachedSignatureName()))
                .contentType(contentType)
                .size(file.length())
                .tempFileLocation(tempFile)
                .description("Business document detached signature")
                .build();
        } catch (Exception e) {
            throw new ConnectorInternalServerException(e.getMessage());
        }
    }

    private FileUploadCommand toFileUploadCommand(
        DomibusConnectorMessageContentType businessContent) {
        try {
            var xmlContent = businessContent.getXmlContent();
            var tempFile = attachmentHelpers.sourceToTempFile(xmlContent);
            var sourceName = ConnectorSOAPMessageAttachmentHelpers.getSourceFileName(xmlContent);
            var filename = StringUtils.hasText(sourceName)
                ? StringUtils.getFilename(StringUtils.cleanPath(sourceName))
                : "businessContent.xml";
            var file = tempFile.toFile();

            return FileUploadCommand
                .builder()
                .filename(filename.endsWith("default.tmp") ? "businessContent.xml" : filename)
                .contentType(MediaType.APPLICATION_XML_VALUE)
                .size(file.length())
                .tempFileLocation(tempFile)
                .description("Registered business content")
                .build();
        } catch (Exception e) {
            throw new ConnectorInternalServerException(e.getMessage());
        }
    }

    private List<FileUploadCommand> toFileUploadCommands(
        List<DomibusConnectorMessageAttachmentType> attachments) {
        return attachments
            .stream()
            .map(attachment -> {
                     try {
                         var tempFile = attachmentHelpers.dataHandlerToTempFile(
                             attachment.getAttachment());

                         return FileUploadCommand
                             .builder()
                             .filename(attachment.getName())
                             .contentType(attachment.getMimeType())
                             .size(tempFile.toFile().length())
                             .tempFileLocation(tempFile)
                             .description("Registered attachment")
                             .build();
                     } catch (Exception e) {
                         throw new ConnectorInternalServerException(e.getMessage());
                     }
                 }
            )
            .toList();
    }

    private String getContentType(Path tempFile) throws IOException {
        var contentType = tika.detect(tempFile);

        return contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private List<ConnectorMessageError> toDomainErrors(
        List<DomibusConnectorMessageErrorType> errors) {
        if (errors == null) {
            return new ArrayList<>();
        }

        return errors.stream().map(
            error -> ConnectorMessageError.builder()
                                          .label(error.getErrorMessage())
                                          .details(error.getErrorDetails())
                                          .source(error.getErrorSource())
                                          .build()).toList();
    }

    private String getBackendClientName() {
        var userPrincipal = webServiceContext.getUserPrincipal();
        var certificateDn = userPrincipal != null ? userPrincipal.getName() : null;

        if (userPrincipal == null || !StringUtils.hasLength(certificateDn)) {
            var error = String.format(
                "Cannot handle request because userPrincipal is [%s] the certificate DN "
                    + "is [%s].",
                userPrincipal,
                certificateDn
            );
            log.error(error);
            throw new IllegalStateException(error);
        }

        var backendClientName = this.backendClientVerifier.getBackendClient(certificateDn);

        if (backendClientName == null) {
            var error = String.format(
                "Cannot handle request because the certificate DN [%s] is not registered as "
                    + "backend client.",
                certificateDn
            );
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.debug("Link partner name is [{}]", backendClientName);

        return backendClientName;
    }
}
