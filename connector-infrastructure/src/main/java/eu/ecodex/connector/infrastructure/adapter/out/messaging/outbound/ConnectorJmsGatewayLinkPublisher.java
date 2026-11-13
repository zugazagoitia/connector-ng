/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.messaging.outbound;

import eu.ecodex.connector.application.port.api.transport.ConnectorRegisterMessageTransportStep;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.infrastructure.config.property.ConnectorQueueProperties;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Session;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Gateway Implementation of the {@link ConnectorMessageEventPublisher}.
 */
@Slf4j
@Component("connectorJmsGatewayLinkPublisher")
public class ConnectorJmsGatewayLinkPublisher
    implements ConnectorMessageEventPublisher<ConnectorMessage> {
    private static final String CONTENT_TYPE_XML = "text/xml";
    private static final String GATEWAY_MESSAGE_TYPE = "submitMessage";
    private static final String MESSAGE_CONTENT_DESCRIPTION = "messageContent";
    private static final String ASICS_DESCRIPTION = "ASIC-S";
    private static final String XML_TOKEN_DESCRIPTION = "tokenXML";
    private static final String UNKNOWN_DESCRIPTION = "Unknown";

    // TODO add unit tests
    private final JmsTemplate jmsTemplate;
    private final ConnectorMessageAttachmentRepository attachmentRepository;
    private final ConnectorFileStorageProvider fileStorageProvider;
    private final ConnectorRegisterMessageTransportStep registerMessageTransportStep;
    private final ConnectorQueueProperties queueProperties;

    /**
     * Creates a new gateway event publisher.
     *
     * @param jmsTemplate         JMS template used to send messages to the gateway queues
     * @param fileStorageProvider provider used to access stored message files or attachments
     * @param queueProperties     configuration containing the JMS queue destinations
     */
    public ConnectorJmsGatewayLinkPublisher(
        JmsTemplate jmsTemplate,
        ConnectorMessageAttachmentRepository attachmentRepository,
        ConnectorFileStorageProvider fileStorageProvider,
        ConnectorRegisterMessageTransportStep registerMessageTransportStep,
        ConnectorQueueProperties queueProperties) {
        this.jmsTemplate = jmsTemplate;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageProvider = fileStorageProvider;
        this.registerMessageTransportStep = registerMessageTransportStep;
        this.queueProperties = queueProperties;
    }

    @Override
    public void publish(@NonNull ConnectorMessage message) {
        log.info("Submitting message [{}] to gateway link processing queue", message.identifier());

        try {
            registerMessageTransportStep.execute(
                message,
                ConnectorMessageTransportStatus.SUBMITTED
            );
            this.jmsTemplate.send(
                queueProperties.gatewaySubmissionQueue(),
                session -> toMapMessage(message, session)
            );
        } catch (Exception e) {
            log.error(
                "Failed to publish message [{}] to gateway link processing queue",
                message.identifier(), e
            );
            registerMessageTransportStep.execute(
                message,
                ConnectorMessageTransportStatus.FAILED
            );
        }
    }

    private MapMessage toMapMessage(ConnectorMessage message, Session session) throws JMSException {
        var mapMessage = session.createMapMessage();

        var as4Properties = message.as4Properties();

        mapMessage.setStringProperty("messageType", GATEWAY_MESSAGE_TYPE);
        mapMessage.setStringProperty(
            "messageId",
            message.as4Properties().ebmsMessageIdentifier() == null
                ? message.identifier()
                : message.as4Properties().ebmsMessageIdentifier()
        );
        mapMessage.setStringProperty("originalSender", as4Properties.originalSender());
        mapMessage.setStringProperty("finalRecipient", as4Properties.finalRecipient());

        var service = as4Properties.service();
        mapMessage.setStringProperty("service", Objects.requireNonNull(service).name());
        mapMessage.setStringProperty("serviceType", service.type());

        var action = as4Properties.action();
        mapMessage.setStringProperty("action", Objects.requireNonNull(action).name());

        var fromParty = as4Properties.fromParty();
        mapMessage.setStringProperty("fromPartyId", Objects.requireNonNull(fromParty).identifier());
        mapMessage.setStringProperty("fromPartyType", fromParty.identifierType());
        mapMessage.setStringProperty("fromRole", fromParty.role());

        var toParty = as4Properties.toParty();
        mapMessage.setStringProperty("toPartyId", Objects.requireNonNull(toParty).identifier());
        mapMessage.setStringProperty("toPartyType", toParty.identifierType());
        mapMessage.setStringProperty("toRole", toParty.role());

        if (as4Properties.conversationIdentifier() != null) {
            mapMessage.setStringProperty("conversationId", as4Properties.conversationIdentifier());
        }

        if (as4Properties.referenceToIdentifier() != null) {
            mapMessage.setStringProperty("refToMessageId", as4Properties.referenceToIdentifier());
        }

        var counter = buildAttachments(mapMessage, message);

        mapMessage.setBooleanProperty("putAttachmentInQueue", false);
        mapMessage.setStringProperty("totalNumberOfPayloads", String.valueOf(counter));

        return mapMessage;
    }

    private int buildContent(MapMessage mapMessage, ConnectorBusinessMessage message, int counter)
        throws JMSException {
        var content = message.businessContent();

        counter++;

        var payload = this.fileStorageProvider.findByIdentifier(content.xmlContent().identifier());

        writePayload(
            mapMessage,
            counter,
            CONTENT_TYPE_XML,
            MESSAGE_CONTENT_DESCRIPTION,
            MESSAGE_CONTENT_DESCRIPTION,
            payload
        );

        return counter;
    }

    private int buildEvidences(MapMessage mapMessage, ConnectorMessage message, int counter)
        throws JMSException {
        var evidences = message.transportedEvidences();

        if (evidences == null) {
            return counter;
        }

        for (var evidence : evidences) {
            counter++;

            var evidenceName = evidence.type().name();

            writePayload(
                mapMessage,
                counter,
                CONTENT_TYPE_XML,
                evidenceName,
                evidenceName,
                evidence.content()
            );
        }

        return counter;
    }

    private int buildAttachments(MapMessage mapMessage, ConnectorMessage message)
        throws JMSException {
        // TODO: with gateway v5.2, consider passing S3 identifier instead of byte[]
        int counter = 0;

        if (message instanceof ConnectorBusinessMessage businessMessage) {
            counter = buildContent(mapMessage, businessMessage, counter);

            var attachments = this.attachmentRepository.findByMessageIdentifierAndTypes(
                message.identifier(),
                List.of(ConnectorAttachmentType.ASICS, ConnectorAttachmentType.XML_TOKEN)
            );

            for (var attachment : attachments) {
                counter++;
                var payload = this.fileStorageProvider.findByIdentifier(attachment.identifier());
                writePayload(
                    mapMessage,
                    counter,
                    attachment.contentType(),
                    describeAttachment(attachment.type()),
                    attachment.name(),
                    payload
                );
            }
        }

        counter = buildEvidences(mapMessage, message, counter);

        assert message.identifier() != null;

        return counter;
    }

    private String describeAttachment(ConnectorAttachmentType type) {
        return switch (type) {
            case ASICS -> ASICS_DESCRIPTION;
            case XML_TOKEN -> XML_TOKEN_DESCRIPTION;
            default -> UNKNOWN_DESCRIPTION;
        };
    }

    /**
     * Writes a single payload entry into the MapMessage at the given index. All five payload
     * properties (mimeContentId, mimeType, description, name, fileName) plus the bytes are written
     * atomically for this index.
     */
    private void writePayload(
        MapMessage mapMessage,
        int index,
        String mimeType,
        String description,
        String name,
        byte[] data) throws JMSException {
        var prefix = "payload_" + index;
        mapMessage.setStringProperty(prefix + "_mimeContentId", generateCID());
        mapMessage.setStringProperty(prefix + "_mimeType", mimeType);
        mapMessage.setStringProperty(prefix + "_description", description);
        mapMessage.setStringProperty(prefix + "_name", name);
        mapMessage.setStringProperty(prefix + "_fileName", name);
        mapMessage.setBytes(prefix, data);
    }

    private String generateCID() {
        return "cid:payload_" + UUID.randomUUID();
    }
}
