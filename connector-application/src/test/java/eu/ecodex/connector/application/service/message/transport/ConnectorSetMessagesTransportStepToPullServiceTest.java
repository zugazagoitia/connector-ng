/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message.transport;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.EvidenceMessageTestFixtures;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorSetMessagesTransportStepToPullService")
public class ConnectorSetMessagesTransportStepToPullServiceTest {
    private static final String BACKEND_NAME = "backend_alice";

    @Mock
    private ConnectorMessageTransportStepRepository transportStepRepository;
    @Mock
    private ConnectorMessageRepository messageRepository;

    @InjectMocks
    private ConnectorSetMessagesTransportStepToPullService service;

    private ConnectorMessageTransportStep businessStep(String stepId, String messageId) {
        return ConnectorMessageTransportStep
            .builder()
            .identifier(stepId)
            .transportedMessage(
                BusinessMessageTestFixtures.createInboundMessage()
                                           .toBuilder()
                                           .identifier(messageId)
                                           .build()
            )
            .build();
    }

    private ConnectorMessageTransportStep evidenceStep(String stepId, String messageId) {
        return ConnectorMessageTransportStep
            .builder()
            .identifier(stepId)
            .transportedMessage(
                EvidenceMessageTestFixtures.createDeliveryEvidenceMessage()
                                           .toBuilder()
                                           .identifier(messageId)
                                           .build()
            )
            .build();
    }

    @Nested
    @SuppressWarnings("DataFlowIssue")
    @DisplayName("when the input is invalid")
    class WhenInputIsInvalid {
        @Test
        void should_fail_when_the_backend_name_is_null() {
            assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(transportStepRepository, messageRepository);
        }
    }

    @Nested
    @DisplayName("when there is no step ready to be pulled")
    class WhenNoStepIsReady {
        @Test
        void should_not_update_any_message() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(List.of());

            service.execute(BACKEND_NAME);

            verify(transportStepRepository).updateStatus(
                List.of(),
                ConnectorMessageTransportStatus.PULLED
            );
            verify(transportStepRepository, never()).findByIdentifier(any());
            verifyNoInteractions(messageRepository);
        }
    }

    @Nested
    @DisplayName("when steps are ready to be pulled")
    class WhenStepsAreReady {
        @Test
        void should_set_the_ready_steps_as_pulled() {
            var stepIds = List.of("step-1", "step-2");
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(stepIds);
            when(transportStepRepository.findByIdentifier("step-1"))
                .thenReturn(businessStep("step-1", "msg-1"));
            when(transportStepRepository.findByIdentifier("step-2"))
                .thenReturn(evidenceStep("step-2", "msg-2"));

            service.execute(BACKEND_NAME);

            verify(transportStepRepository).findReadyToDownloadTransportStepsIds(BACKEND_NAME);
            verify(transportStepRepository).updateStatus(
                stepIds,
                ConnectorMessageTransportStatus.PULLED
            );
        }

        @Test
        void should_set_the_business_messages_as_pulled() {
            var stepIds = List.of("step-1", "step-2");
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(stepIds);
            when(transportStepRepository.findByIdentifier("step-1"))
                .thenReturn(businessStep("step-1", "msg-1"));
            when(transportStepRepository.findByIdentifier("step-2"))
                .thenReturn(businessStep("step-2", "msg-2"));

            service.execute(BACKEND_NAME);

            verify(messageRepository).updateStatus("msg-1", ConnectorBusinessMessageStatus.PULLED);
            verify(messageRepository).updateStatus("msg-2", ConnectorBusinessMessageStatus.PULLED);
            verifyNoMoreInteractions(messageRepository);
        }

        @Test
        void should_not_update_the_message_status_when_the_transported_message_is_an_evidence() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(List.of("step-1"));
            when(transportStepRepository.findByIdentifier("step-1"))
                .thenReturn(evidenceStep("step-1", "msg-1"));

            service.execute(BACKEND_NAME);

            verify(transportStepRepository).updateStatus(
                List.of("step-1"),
                ConnectorMessageTransportStatus.PULLED
            );
            verifyNoInteractions(messageRepository);
        }

        @Test
        void should_only_update_the_status_of_the_business_messages_when_steps_are_mixed() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(List.of("step-1", "step-2", "step-3"));
            when(transportStepRepository.findByIdentifier("step-1"))
                .thenReturn(evidenceStep("step-1", "msg-1"));
            when(transportStepRepository.findByIdentifier("step-2"))
                .thenReturn(businessStep("step-2", "msg-2"));
            when(transportStepRepository.findByIdentifier("step-3"))
                .thenReturn(evidenceStep("step-3", "msg-3"));

            service.execute(BACKEND_NAME);

            verify(messageRepository).updateStatus("msg-2", ConnectorBusinessMessageStatus.PULLED);
            verifyNoMoreInteractions(messageRepository);
        }

        @Test
        void should_only_look_up_the_steps_of_the_requested_backend() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(List.of());

            service.execute(BACKEND_NAME);

            verify(transportStepRepository).findReadyToDownloadTransportStepsIds(BACKEND_NAME);
            verify(transportStepRepository, never())
                .findReadyToDownloadTransportStepsIds(argThat(name -> !BACKEND_NAME.equals(name)));
        }

        @Test
        void should_set_the_steps_as_pulled_before_updating_the_messages() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(List.of("step-1"));
            when(transportStepRepository.findByIdentifier("step-1"))
                .thenReturn(businessStep("step-1", "msg-1"));

            service.execute(BACKEND_NAME);

            var orders = inOrder(transportStepRepository, messageRepository);
            orders.verify(transportStepRepository).updateStatus(
                List.of("step-1"),
                ConnectorMessageTransportStatus.PULLED
            );
            orders.verify(messageRepository).updateStatus(
                "msg-1",
                ConnectorBusinessMessageStatus.PULLED
            );
        }
    }
}
