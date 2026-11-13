/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.application.exception.evidence.ConnectorEvidenceException;
import eu.ecodex.connector.application.exception.evidence.ConnectorEvidenceNotRelevantException;
import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.ConnectorMessageResolvedEvent;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorEvidenceType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorMessageEvidenceVerifierService")
public class ConnectorMessageEvidenceVerifierServiceTest {
    private static final String ALREADY_REJECTED_MESSAGE =
        "The processed evidence is ignored, because the business message is "
            + "already in rejected state";

    @Mock
    private ConnectorMessageRepository messageRepository;
    @Mock
    private ConnectorMessageEventPublisher<ConnectorMessageResolvedEvent>
        attachmentCleanupPublisher;

    @InjectMocks
    private ConnectorMessageEvidenceVerifierService messageEvidenceVerifierService;

    @Nested
    @DisplayName("when the evidence is confirmation")
    class WhenConfirming {
        @ParameterizedTest
        @EnumSource(value = ConnectorEvidenceType.class, names = {"DELIVERY", "RETRIEVAL"})
        void should_process_the_message_as_confirmed(ConnectorEvidenceType evidenceType) {
            var message =
                BusinessMessageTestFixtures.createMessageWithSubmissionAcceptanceEvidence();
            when(messageRepository.findByIdentifier(any())).thenReturn(message);

            messageEvidenceVerifierService.verify(evidenceType, message);

            verify(messageRepository).setAsConfirmed(message.identifier());
            verify(messageRepository, never()).setAsRejected(any());
        }

        @ParameterizedTest
        @EnumSource(value = ConnectorEvidenceType.class, names = {"DELIVERY", "RETRIEVAL"})
        void should_publish_the_resolved_event_when_the_message_is_confirmed(
            ConnectorEvidenceType evidenceType) {
            var message =
                BusinessMessageTestFixtures.createMessageWithSubmissionAcceptanceEvidence();
            when(messageRepository.findByIdentifier(any())).thenReturn(message);

            messageEvidenceVerifierService.verify(evidenceType, message);

            verify(attachmentCleanupPublisher)
                .publish(new ConnectorMessageResolvedEvent(message.identifier()));
        }

        @Test
        void should_confirm_the_message_when_the_stored_message_is_not_found() {
            var message =
                BusinessMessageTestFixtures.createMessageWithSubmissionAcceptanceEvidence();
            when(messageRepository.findByIdentifier(any())).thenReturn(null);

            messageEvidenceVerifierService.verify(ConnectorEvidenceType.DELIVERY, message);

            verify(messageRepository).setAsConfirmed(message.identifier());
            verify(attachmentCleanupPublisher)
                .publish(new ConnectorMessageResolvedEvent(message.identifier()));
        }

        @Test
        void should_confirm_the_message_before_publishing_the_resolved_event() {
            var message =
                BusinessMessageTestFixtures.createMessageWithSubmissionAcceptanceEvidence();
            when(messageRepository.findByIdentifier(any())).thenReturn(message);

            messageEvidenceVerifierService.verify(ConnectorEvidenceType.DELIVERY, message);

            var orders = inOrder(messageRepository, attachmentCleanupPublisher);
            orders.verify(messageRepository).setAsConfirmed(message.identifier());
            orders.verify(attachmentCleanupPublisher)
                   .publish(new ConnectorMessageResolvedEvent(message.identifier()));
        }

        @Test
        void should_propagate_the_failure_of_the_publisher() {
            var message =
                BusinessMessageTestFixtures.createMessageWithSubmissionAcceptanceEvidence();
            when(messageRepository.findByIdentifier(any())).thenReturn(message);
            doThrow(new IllegalStateException("JMS unavailable"))
                .when(attachmentCleanupPublisher).publish(any());

            var exception = assertThrows(
                IllegalStateException.class,
                () -> messageEvidenceVerifierService.verify(
                    ConnectorEvidenceType.DELIVERY, message)
            );

            assertThat(exception.getMessage()).isEqualTo("JMS unavailable");
        }
    }

    @Nested
    @DisplayName("when the evidence is rejection")
    class WhenRejecting {
        @ParameterizedTest
        @EnumSource(
            value = ConnectorEvidenceType.class,
            names = {
                "SUBMISSION_REJECTION",
                "NON_DELIVERY",
                "NON_RETRIEVAL",
                "RELAY_REMMD_REJECTION",
                "RELAY_REMMD_FAILURE"
            }
        )
        void should_process_the_message_as_rejected(ConnectorEvidenceType evidenceType) {
            var message = BusinessMessageTestFixtures.createRejectedMessage();

            messageEvidenceVerifierService.verify(evidenceType, message);

            verify(messageRepository).setAsRejected(message.identifier());
            verify(messageRepository, never()).setAsConfirmed(any());
            verify(messageRepository, never()).findByIdentifier(any());
        }

        @ParameterizedTest
        @EnumSource(
            value = ConnectorEvidenceType.class,
            names = {
                "SUBMISSION_REJECTION",
                "NON_DELIVERY",
                "NON_RETRIEVAL",
                "RELAY_REMMD_REJECTION",
                "RELAY_REMMD_FAILURE"
            }
        )
        void should_publish_the_resolved_event_when_the_message_is_rejected(
            ConnectorEvidenceType evidenceType) {
            var message = BusinessMessageTestFixtures.createRejectedMessage();

            messageEvidenceVerifierService.verify(evidenceType, message);

            verify(attachmentCleanupPublisher)
                .publish(new ConnectorMessageResolvedEvent(message.identifier()));
        }

        @Test
        void should_reject_the_message_before_publishing_the_resolved_event() {
            var message = BusinessMessageTestFixtures.createRejectedMessage();

            messageEvidenceVerifierService.verify(ConnectorEvidenceType.NON_DELIVERY, message);

            var inOrder = inOrder(messageRepository, attachmentCleanupPublisher);
            inOrder.verify(messageRepository).setAsRejected(message.identifier());
            inOrder.verify(attachmentCleanupPublisher)
                   .publish(new ConnectorMessageResolvedEvent(message.identifier()));
        }
    }

    @Nested
    @DisplayName("when the evidence is a relay REMMD acceptance")
    class WhenRelayRemmdAcceptance {
        @Test
        void should_mark_the_message_as_received_by_the_opposite_connector_when_it_goes_to_the_gateway() {
            var message =
                BusinessMessageTestFixtures.createMessageWithSubmissionAcceptanceEvidence()
                                           .toBuilder()
                                           .direction(ConnectorMessageDirection.BACKEND_TO_GATEWAY)
                                           .build();

            messageEvidenceVerifierService.verify(
                ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE,
                message
            );

            verify(messageRepository).updateStatus(
                message.identifier(),
                ConnectorBusinessMessageStatus.RECEIVED_BY_OPPOSITE_CONNECTOR
            );
            verifyNoMoreInteractions(messageRepository);
            verifyNoInteractions(attachmentCleanupPublisher);
        }

        @Test
        void should_not_update_the_message_when_it_goes_to_the_backend() {
            var message =
                BusinessMessageTestFixtures.createMessageWithSubmissionAcceptanceEvidence()
                                           .toBuilder()
                                           .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                           .build();

            messageEvidenceVerifierService.verify(
                ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE,
                message
            );

            verifyNoInteractions(messageRepository, attachmentCleanupPublisher);
        }
    }

    @Nested
    @DisplayName("when the evidence neither confirms nor rejects the message")
    class WhenNeitherConfirmingNorRejecting {
        @Test
        void should_leave_the_message_untouched() {
            var message =
                BusinessMessageTestFixtures.createMessageWithSubmissionAcceptanceEvidence();

            messageEvidenceVerifierService.verify(
                ConnectorEvidenceType.SUBMISSION_ACCEPTANCE,
                message
            );

            verifyNoInteractions(messageRepository, attachmentCleanupPublisher);
        }
    }

    @Nested
    @DisplayName("when the evidence is not relevant")
    class WhenNotRelevant {
        @ParameterizedTest
        @EnumSource(value = ConnectorEvidenceType.class, names = {"DELIVERY", "RETRIEVAL"})
        void should_fail_when_the_message_is_already_rejected(ConnectorEvidenceType evidenceType) {
            var message = BusinessMessageTestFixtures.createRejectedMessage();
            when(messageRepository.findByIdentifier(any())).thenReturn(message);

            var exception = assertThrows(
                ConnectorEvidenceNotRelevantException.class,
                () -> messageEvidenceVerifierService.verify(evidenceType, message)
            );

            assertThat(exception.getMessage()).contains(ALREADY_REJECTED_MESSAGE);
        }

        @ParameterizedTest
        @EnumSource(value = ConnectorEvidenceType.class, names = {"DELIVERY", "RETRIEVAL"})
        void should_not_confirm_nor_publish_when_the_message_is_already_rejected(
            ConnectorEvidenceType evidenceType) {
            var message = BusinessMessageTestFixtures.createRejectedMessage();
            when(messageRepository.findByIdentifier(any())).thenReturn(message);

            assertThrows(
                ConnectorEvidenceNotRelevantException.class,
                () -> messageEvidenceVerifierService.verify(evidenceType, message)
            );

            verify(messageRepository, never()).setAsConfirmed(any());
            verify(messageRepository, never()).setAsRejected(any());
            verifyNoInteractions(attachmentCleanupPublisher);
        }

        @Test
        void should_fail_when_the_evidence_priority_is_lower_than_the_current_highest() {
            var message = BusinessMessageTestFixtures.createRejectedMessage();

            assertThrows(
                ConnectorEvidenceNotRelevantException.class,
                () -> messageEvidenceVerifierService.verify(
                    ConnectorEvidenceType.SUBMISSION_ACCEPTANCE,
                    message
                )
            );
        }

        @Test
        void should_not_update_nor_publish_when_the_evidence_priority_is_lower_than_the_current_highest() {
            var message = BusinessMessageTestFixtures.createRejectedMessage();

            assertThrows(
                ConnectorEvidenceNotRelevantException.class,
                () -> messageEvidenceVerifierService.verify(
                    ConnectorEvidenceType.SUBMISSION_ACCEPTANCE,
                    message
                )
            );

            verifyNoInteractions(messageRepository, attachmentCleanupPublisher);
        }
    }

    @Nested
    @DisplayName("when the input is invalid")
    class WhenInputIsInvalid {
        @Test
        void should_fail_when_the_transported_evidences_are_null() {
            var message = BusinessMessageTestFixtures.createRejectedMessage()
                                                     .toBuilder()
                                                     .transportedEvidences(null)
                                                     .build();

            assertThrows(
                ConnectorEvidenceException.class,
                () -> messageEvidenceVerifierService.verify(
                    ConnectorEvidenceType.SUBMISSION_ACCEPTANCE,
                    message
                )
            );

            verifyNoInteractions(messageRepository, attachmentCleanupPublisher);
        }

        @Test
        void should_fail_when_the_transported_evidences_are_empty() {
            var message = BusinessMessageTestFixtures.createRejectedMessage()
                                                     .toBuilder()
                                                     .transportedEvidences(List.of())
                                                     .build();

            assertThrows(
                ConnectorEvidenceException.class,
                () -> messageEvidenceVerifierService.verify(
                    ConnectorEvidenceType.SUBMISSION_ACCEPTANCE,
                    message
                )
            );

            verifyNoInteractions(messageRepository, attachmentCleanupPublisher);
        }

        @Test
        void should_fail_when_the_evidence_type_is_null() {
            var message = BusinessMessageTestFixtures.createOutboundMessage();

            assertThrows(
                NullPointerException.class,
                () -> messageEvidenceVerifierService.verify(null, message)
            );

            verifyNoInteractions(messageRepository, attachmentCleanupPublisher);
        }

        @Test
        void should_fail_when_the_message_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> messageEvidenceVerifierService.verify(
                    ConnectorEvidenceType.SUBMISSION_ACCEPTANCE,
                    null
                )
            );

            verifyNoInteractions(messageRepository, attachmentCleanupPublisher);
        }
    }
}
