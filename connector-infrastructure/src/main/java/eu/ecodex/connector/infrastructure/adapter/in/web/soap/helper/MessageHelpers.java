/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.soap.helper;

import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundBusinessMessageCommand;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundEvidenceMessageCommand;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomain;
import eu.ecodex.connector.domain.model.message.ConnectorMessageAS4Properties;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorBusinessDocumentAESType;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessDocument;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorEvidenceType;
import eu.ecodex.connector.domain.model.pmode.ConnectorPartyRoleType;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeAction;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeService;
import eu.ecodex.connector.domain.transition.DomibusConnectorDetachedSignatureType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageConfirmationType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageContentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageDetailsType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageType;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
public class MessageHelpers {
    /**
     * Determines if the given message is an evidence trigger request. A message qualifies as an
     * evidence trigger request if it has no message content, contains exactly one message
     * confirmation, and the confirmation payload is empty.
     *
     * @param message The {@link DomibusConnectorMessageType} to check for being an evidence trigger
     *                request. This parameter should not be null and represents the message whose
     *                metadata and confirmations are inspected.
     *
     * @return {@code true} if the message is an evidence trigger request; otherwise, {@code false}.
     */
    public static boolean isEvidenceTriggerRequest(DomibusConnectorMessageType message) {
        if (message.getMessageContent() != null) {
            return false;
        }
        var confirmations = message.getMessageConfirmations();
        return confirmations != null
            && confirmations.size() == 1
            && isEmptyConfirmationPayload(confirmations.getFirst());
    }

    public static ConnectorOutboundBusinessMessageCommand toBusinessCommand(
        DomibusConnectorMessageType message,
        List<String> attachments,
        String businessContentAttachmentIdentifier,
        String businessDocumentAttachmentIdentifier,
        String businessDocDetachedSigAttachmentId,
        String backendClientName
    ) {
        var details = message.getMessageDetails();
        var incomingBusinessContent = message.getMessageContent();

        return ConnectorOutboundBusinessMessageCommand
            .builder()
            .backendMessageIdentifier(details.getBackendMessageId())
            .backendName(backendClientName)
            .businessDomainIdentifier(ConnectorBusinessDomain.DEFAULT_BUSINESS_DOMAIN_ID)
            .as4Properties(toAS4Properties(details))
            .businessContent(toBusinessContent(
                incomingBusinessContent,
                businessContentAttachmentIdentifier,
                businessDocumentAttachmentIdentifier,
                businessDocDetachedSigAttachmentId
            ))
            .direction(ConnectorMessageDirection.BACKEND_TO_GATEWAY)
            .attachments(toAttachments(attachments))
            .build();
    }

    public static ConnectorOutboundEvidenceMessageCommand toEvidenceTriggerCommand(
        DomibusConnectorMessageType message,
        String backendClientName) {
        var details = message.getMessageDetails();
        var confirmation = message.getMessageConfirmations().getFirst();

        return ConnectorOutboundEvidenceMessageCommand
            .builder()
            .evidenceType(ConnectorEvidenceType.valueOf(confirmation.getConfirmationType().value()))
            .backendName(backendClientName)
            .referenceToMessageIdentifier(details.getRefToMessageId())
            .build();
    }

    public static ConnectorMessageAS4Properties toAS4Properties(
        DomibusConnectorMessageDetailsType details) {
        var service = ConnectorProcessingModeService
            .builder()
            .name(details.getService().getService())
            .type(details.getService().getServiceType())
            .build();
        var action = ConnectorProcessingModeAction
            .builder()
            .name(details.getAction().getAction())
            .build();
        var fromParty = ConnectorProcessingModeParty
            .builder()
            .identifier(details.getFromParty().getPartyId())
            .identifierType(details.getFromParty().getPartyIdType())
            .role(details.getFromParty().getRole())
            .roleType(ConnectorPartyRoleType.INITIATOR)
            .build();
        var toParty = ConnectorProcessingModeParty
            .builder()
            .identifier(details.getToParty().getPartyId())
            .identifierType(details.getToParty().getPartyIdType())
            .role(details.getToParty().getRole())
            .roleType(ConnectorPartyRoleType.RESPONDER)
            .build();

        return ConnectorMessageAS4Properties
            .builder()
            .conversationIdentifier(details.getConversationId())
            .ebmsMessageIdentifier(details.getEbmsMessageId())
            .referenceToIdentifier(details.getRefToMessageId())
            .originalSender(details.getOriginalSender())
            .finalRecipient(details.getFinalRecipient())
            .service(service)
            .action(action)
            .fromParty(fromParty)
            .toParty(toParty)
            .build();
    }

    public static ConnectorMessageBusinessContent toBusinessContent(
        DomibusConnectorMessageContentType content,
        String businessContentAttachmentIdentifier,
        String businessDocumentAttachmentIdentifier,
        String businessDocDetachedSigAttachmentId) {
        var document = content.getDocument();
        var businessDocument = ConnectorMessageBusinessDocument
            .builder()
            .content(toAttachment(businessDocumentAttachmentIdentifier))
            .detachedSignature(toDetachedSignature(
                document.getDetachedSignature(),
                businessDocDetachedSigAttachmentId
            ))
            .aesType(ConnectorBusinessDocumentAESType.from(
                         document.getAesType() != null
                             ? document.getAesType().value()
                             : null
                     )
            )
            .build();

        return ConnectorMessageBusinessContent
            .builder()
            .xmlContent(toAttachment(businessContentAttachmentIdentifier))
            .businessDocument(businessDocument)
            .build();
    }

    private static boolean isEmptyConfirmationPayload(
        DomibusConnectorMessageConfirmationType confirmation) {
        var source = confirmation.getConfirmation();

        if (source == null) {
            return true;
        }

        if (source instanceof StreamSource streamSource) {
            var reader = streamSource.getReader();
            if (reader != null) {
                try {
                    return !reader.ready();
                } catch (Exception e) {
                    return true;
                }
            }
            var stream = streamSource.getInputStream();
            if (stream != null) {
                try {
                    return stream.available() == 0;
                } catch (Exception e) {
                    return true;
                }
            }
        }

        return false;
    }

    private static ConnectorMessageAttachment toDetachedSignature(
        DomibusConnectorDetachedSignatureType signature,
        String businessDocDetachedSigAttachmentId) {

        if (signature == null) {
            return null;
        }

        return toAttachment(businessDocDetachedSigAttachmentId);
    }

    private static ConnectorMessageAttachment toAttachment(String identifier) {
        return ConnectorMessageAttachment
            .builder()
            .identifier(identifier)
            .build();
    }

    private static List<ConnectorMessageAttachment> toAttachments(List<String> identifiers) {
        if (identifiers == null) {
            return new ArrayList<>();
        }

        return identifiers.stream()
                          .map(MessageHelpers::toAttachment)
                          .toList();
    }
}
