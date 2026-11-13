/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.messaging.inbound;

import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.infrastructure.config.property.ConnectorQueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Event publisher responsible for emitting an inbound message staging events.
 *
 * <p>This class implements {@link ConnectorMessageEventPublisher} and is used to publish events
 * when a {@link ConnectorBusinessMessage} enters the inbound staging phase.
 */
@Slf4j
@Component
public class ConnectorJmsInboundMessageStagingPublisher
    implements ConnectorMessageEventPublisher<ConnectorBusinessMessage> {
    private final JmsTemplate jmsTemplate;
    private final ConnectorQueueProperties queueProperties;

    public ConnectorJmsInboundMessageStagingPublisher(
        JmsTemplate jmsTemplate,
        ConnectorQueueProperties queueProperties) {
        this.jmsTemplate = jmsTemplate;
        this.queueProperties = queueProperties;
    }

    @Override
    public void publish(@NonNull ConnectorBusinessMessage message) {
        log.info("Submitting message [{}] to inbound message staging queue", message.identifier());

        this.jmsTemplate.convertAndSend(
            this.queueProperties.inboundMessageStagingQueue(),
            message
        );
    }
}
