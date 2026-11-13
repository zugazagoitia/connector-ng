/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.messaging.inbound;

import eu.ecodex.connector.application.port.api.message.inbound.ConnectorInboundBusinessMessageCommand;
import eu.ecodex.connector.application.port.api.message.inbound.ConnectorInboundBusinessMessageReceiver;
import eu.ecodex.connector.application.port.api.message.inbound.ConnectorInboundEvidenceMessageCommand;
import eu.ecodex.connector.application.port.api.message.inbound.ConnectorInboundEvidenceMessageReceiver;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.ConnectorDefaults;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomain;
import eu.ecodex.connector.domain.model.message.ConnectorMessageAS4Properties;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorEvidenceType;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import eu.ecodex.connector.domain.model.pmode.ConnectorPartyRoleType;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeAction;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeService;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * JMS listener responsible for handling message submitted by the gateway to the connector.
 */
@Slf4j
@Component
public class ConnectorJmsGatewayMessageListener {
    private static final String CONTENT_TYPE_XML = "application/xml";
    private static final String CONTENT_TYPE_ASICS = "application/vnd.etsi.asic-s+zip";
    private static final String MESSAGE_CONTENT_DESCRIPTION = "messageContent";
    private static final String ASICS_DESCRIPTION = "ASIC-S";
    private static final String XML_TOKEN_DESCRIPTION = "tokenXML";
    private static final String GATEWAY_MESSAGE_TYPE = "incomingMessage";

    private static final Set<String> EVIDENCE_TYPE_NAMES =
        Arrays.stream(ConnectorEvidenceType.values())
              .map(Enum::name)
              .collect(Collectors.toUnmodifiableSet());

    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorFileStorageProvider fileStorageProvider;
    private final ConnectorInboundBusinessMessageReceiver inboundBusinessMessageReceiver;
    private final ConnectorInboundEvidenceMessageReceiver inboundEvidenceMessageReceiver;


    /**
     * Constructs a new instance of the {@link ConnectorJmsGatewayMessageListener} with the
     * specified dependencies.
     *
     * @param attachmentRepository           The repository for managing message attachments.
     * @param fileStorageProvider            The provider for file storage operations.
     * @param inboundBusinessMessageReceiver The service for handling inbound business messages.
     * @param inboundEvidenceMessageReceiver The service for handling inbound evidence messages.
     */
    public ConnectorJmsGatewayMessageListener(
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorFileStorageProvider fileStorageProvider,
        ConnectorInboundBusinessMessageReceiver inboundBusinessMessageReceiver,
        ConnectorInboundEvidenceMessageReceiver inboundEvidenceMessageReceiver) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageProvider = fileStorageProvider;
        this.inboundBusinessMessageReceiver = inboundBusinessMessageReceiver;
        this.inboundEvidenceMessageReceiver = inboundEvidenceMessageReceiver;
    }

    /**
     * Handles incoming messages from the gateway reception queue. This method processes the
     * received JMS message, validates its headers, parses its content, and persists the information
     * as a domain-specific message representation. The processed message is then published as part
     * of a pipeline event.
     *
     * @param message the JMS MapMessage received from the gateway. It must not be null and must
     *                contain valid message headers and payloads to be correctly processed.
     *
     * @throws JMSException if an error occurs during processing, such as issues with accessing
     *                      message properties or payloads.
     */
    @Transactional
    @JmsListener(destination = "${connector.queues.gateway-reception-queue}")
    public void handle(@NonNull MapMessage message) throws JMSException {
        log.info("Receiving message from the gateway");

        validateMessageHeader(message);

        var as4Properties = parseAS4Properties(message);
        var payloads = parsePayloads(message);

        if (payloads.transportedEvidences().isEmpty()) {
            throw new IllegalArgumentException(
                "Incoming message requires at least one transported evidence"
            );
        }

        if (payloads.businessContent() != null) {
            log.info("Received message from the gateway is a business message");
            var businessMessageCommand = ConnectorInboundBusinessMessageCommand
                .builder()
                .businessDomainIdentifier(ConnectorBusinessDomain.DEFAULT_BUSINESS_DOMAIN_ID)
                .as4Properties(as4Properties)
                .gatewayName(ConnectorDefaults.DEFAULT_GATEWAY_NAME)
                .businessContent(payloads.businessContent())
                .attachments(payloads.attachments())
                .transportedEvidences(payloads.transportedEvidences())
                .build();

            inboundBusinessMessageReceiver.execute(businessMessageCommand);
        } else {
            log.info("Received message from the gateway is an evidence message");
            var inboundMessageCommand = ConnectorInboundEvidenceMessageCommand
                .builder()
                .businessDomainIdentifier(ConnectorBusinessDomain.DEFAULT_BUSINESS_DOMAIN_ID)
                .as4Properties(as4Properties)
                .gatewayName(ConnectorDefaults.DEFAULT_GATEWAY_NAME)
                .transportedEvidences(payloads.transportedEvidences())
                .build();
            inboundEvidenceMessageReceiver.execute(inboundMessageCommand);
        }
    }

    private void validateMessageHeader(MapMessage message) throws JMSException {
        var messageType = message.getStringProperty("messageType");

        if (!GATEWAY_MESSAGE_TYPE.equals(messageType)) {
            throw new IllegalArgumentException(
                "Invalid Gateway reception messageType: " + messageType);
        }

        var ebmsMessageIdentifier = message.getStringProperty("messageId");

        if (!StringUtils.hasText(ebmsMessageIdentifier)) {
            throw new IllegalArgumentException(
                "Invalid Gateway reception messageId: " + ebmsMessageIdentifier);
        }

        var total = message.getIntProperty("totalNumberOfPayloads");

        if (total <= 0) {
            throw new IllegalArgumentException(
                "Invalid Gateway reception totalNumberOfPayloads: " + total);
        }
    }

    private ConnectorMessageAS4Properties parseAS4Properties(MapMessage message)
        throws JMSException {
        var service = ConnectorProcessingModeService.builder()
                                                    .name(message.getStringProperty("service"))
                                                    .type(message.getStringProperty("serviceType"))
                                                    .build();
        var action = ConnectorProcessingModeAction.builder()
                                                  .name(message.getStringProperty("action"))
                                                  .build();

        var fromParty = buildParty(message, "from", ConnectorPartyRoleType.INITIATOR);
        var toParty = buildParty(message, "to", ConnectorPartyRoleType.RESPONDER);

        return ConnectorMessageAS4Properties
            .builder()
            .service(service)
            .action(action)
            .fromParty(fromParty)
            .toParty(toParty)
            .conversationIdentifier(message.getStringProperty("conversationId"))
            .ebmsMessageIdentifier(message.getStringProperty("messageId"))
            .referenceToIdentifier(message.getStringProperty("refToMessageId"))
            .originalSender(message.getStringProperty("originalSender"))
            .finalRecipient(message.getStringProperty("finalRecipient"))
            .build();
    }

    private ConnectorProcessingModeParty buildParty(
        MapMessage message,
        String prefix,
        ConnectorPartyRoleType roleType)
        throws JMSException {
        var id = message.getStringProperty(prefix + "PartyId");
        var type = message.getStringProperty(prefix + "PartyType");
        var role = message.getStringProperty(prefix + "Role");

        if (!StringUtils.hasText(id) || !StringUtils.hasText(type) || !StringUtils.hasText(role)) {
            throw new IllegalArgumentException(
                "[%sParty] is not allowed to be null".formatted(prefix)
            );
        }
        return ConnectorProcessingModeParty.builder()
                                           .identifier(id)
                                           .identifierType(type)
                                           .role(role)
                                           .roleType(roleType)
                                           .build();
    }

    private ParsedPayloads parsePayloads(MapMessage message) throws JMSException {
        var total = message.getIntProperty("totalNumberOfPayloads");
        var attachments = new ArrayList<ConnectorMessageAttachment>();
        var evidences = new ArrayList<ConnectorMessageEvidence>();
        ConnectorMessageBusinessContent businessContent = null;

        for (var i = 1; i <= total; i++) {
            var prefix = "payload_" + i;
            var description = message.getStringProperty(prefix + "_description");
            var name = message.getStringProperty(prefix + "_name");
            var payload = message.getBytes(prefix);

            if (!StringUtils.hasText(description)) {
                throw new IllegalArgumentException(
                    "Missing description for payload at index %d".formatted(i)
                );
            }

            var resolvedName = StringUtils.hasText(name)
                ? name
                : description.toLowerCase(Locale.ROOT);

            if (MESSAGE_CONTENT_DESCRIPTION.equalsIgnoreCase(description)) {
                var content = saveAndUploadAttachment(
                    resolvedName,
                    CONTENT_TYPE_XML,
                    "Inbound message business content",
                    ConnectorAttachmentType.BUSINESS_CONTENT,
                    payload
                );
                // Business document is bundled with ASICS payload
                businessContent = ConnectorMessageBusinessContent.builder()
                                                                 .xmlContent(content)
                                                                 .businessDocument(null)
                                                                 .build();
            } else if (ASICS_DESCRIPTION.equalsIgnoreCase(description)) {
                attachments.add(saveAndUploadAttachment(
                    resolvedName,
                    CONTENT_TYPE_ASICS,
                    "Inbound message ASIC-S component",
                    ConnectorAttachmentType.ASICS,
                    payload
                ));
            } else if (XML_TOKEN_DESCRIPTION.equalsIgnoreCase(description)) {
                attachments.add(saveAndUploadAttachment(
                    resolvedName,
                    CONTENT_TYPE_XML,
                    "Inbound message XML Trust OK Token",
                    ConnectorAttachmentType.XML_TOKEN,
                    payload
                ));
            } else if (EVIDENCE_TYPE_NAMES.contains(description.toUpperCase(Locale.ROOT))) {
                evidences.add(ConnectorMessageEvidence.builder()
                                                      .type(ConnectorEvidenceType.valueOf(
                                                          description.toUpperCase(Locale.ROOT)))
                                                      .content(payload)
                                                      .build());
            } else {
                log.warn(
                    "Unrecognised payload description '{}' at index {} — skipping",
                    description, i
                );
            }
        }

        return new ParsedPayloads(businessContent, attachments, evidences);
    }

    private ConnectorMessageAttachment saveAndUploadAttachment(
        String name,
        String contentType,
        String description,
        ConnectorAttachmentType type,
        byte[] payload) {
        var identifier = UUID.randomUUID() + "_" + name;
        var attachment = ConnectorMessageAttachment.builder()
                                                   .identifier(identifier)
                                                   .name(name)
                                                   .contentType(contentType)
                                                   .size(payload.length)
                                                   .description(description)
                                                   .storage(ConnectorAttachmentStorage.S3_BUCKET)
                                                   .type(type)
                                                   .build();

        fileStorageProvider.save(attachment, payload);
        return attachmentRepository.save(attachment);
    }

    private record ParsedPayloads(
        ConnectorMessageBusinessContent businessContent,
        List<ConnectorMessageAttachment> attachments,
        List<ConnectorMessageEvidence> transportedEvidences
    ) {
    }
}
