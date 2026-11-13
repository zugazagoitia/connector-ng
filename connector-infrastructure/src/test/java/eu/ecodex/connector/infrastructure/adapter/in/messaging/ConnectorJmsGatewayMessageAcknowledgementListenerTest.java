/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.messaging;


import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.TransportStepFixtures;
import eu.ecodex.connector.application.port.api.transport.ConnectorAckMessageTransportStep;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import eu.ecodex.connector.infrastructure.adapter.in.messaging.inbound.ConnectorJmsGatewayMessageAcknowledgementListener;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@SuppressWarnings("DataFlowIssue")
@DisplayName("ConnectorJmsGatewayMessageAcknowledgementListener")
public class ConnectorJmsGatewayMessageAcknowledgementListenerTest extends BaseJmsMessageTest {
    private static final String MESSAGE_ID = "msg-001";

    @Mock
    private ConnectorMessageTransportStepRepository transportStepRepository;
    @Mock
    private ConnectorAckMessageTransportStep acknowledgeMessageTransportStep;
    @Mock
    private MapMessage mapMessage;

    @InjectMocks
    private ConnectorJmsGatewayMessageAcknowledgementListener listener;

    @Nested
    @DisplayName("invalid JMS message")
    class InvalidMessages {
        @Test
        void should_reject_null_jms_message() {
            assertThatThrownBy(() -> listener.handle(null))
                .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(
                transportStepRepository,
                acknowledgeMessageTransportStep
            );
        }

        @Test
        void should_fail_when_message_identifier_is_missing() throws JMSException {
            when(mapMessage.getStringProperty("messageId")).thenReturn(null);

            assertThatThrownBy(() -> listener.handle(mapMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Message identifier not found");

            verifyNoInteractions(
                transportStepRepository,
                acknowledgeMessageTransportStep
            );
        }
    }

    @Nested
    @DisplayName("gateway acknowledgements")
    class GatewayAcknowledgement {
        @Test
        void should_acknowledge_gateway_reply_successfully() throws JMSException {
            when(mapMessage.getStringProperty("messageId"))
                .thenReturn(MESSAGE_ID);
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(any()))
                .thenReturn(TransportStepFixtures.createTransportStepWithBusinessMessage());
            doNothing()
                .when(acknowledgeMessageTransportStep)
                .execute(any(), any());

            listener.handle(mapMessage);

            verify(transportStepRepository)
                .findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID);
            verify(acknowledgeMessageTransportStep)
                .execute(any(), any());
        }
    }

    @Nested
    @DisplayName("Jms failures")
    class JmsFailures {
        @Test
        void should_fail_when_broker_is_unavailable() throws JMSException {
            when(mapMessage.getStringProperty("messageId"))
                .thenThrow(new JMSException("broker unavailable"));

            assertThatThrownBy(() -> listener.handle(mapMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse Domibus reply")
                .hasCauseInstanceOf(JMSException.class);

            verifyNoInteractions(
                transportStepRepository,
                acknowledgeMessageTransportStep
            );
        }
    }
}
