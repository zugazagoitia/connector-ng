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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.EvidenceMessageTestFixtures;
import eu.ecodex.connector.EvidenceTestFixtures;
import eu.ecodex.connector.application.exception.message.ConnectorLinkPartnerNotOwnerException;
import eu.ecodex.connector.application.exception.message.ConnectorMessageTransportStepNotFoundException;
import eu.ecodex.connector.application.port.api.transport.command.UpdateMessageTransportCommand;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageErrorRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageEvidenceRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorEvidenceMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.ConnectorMessageError;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStepStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorAckMessageTransportStepService")
public class ConnectorAckMessageTransportStepServiceTest {
    private static final String MESSAGE_ID = "msg-001";
    private static final String TRANSPORT_STEP_ID = "step-001";
    private static final String BACKEND_ID = "backend-ref-001";
    private static final String LINK_PARTNER = "default_test_backend";

    @Mock
    private ConnectorMessageTransportStepRepository transportStepRepository;
    @Mock
    private ConnectorMessageRepository messageRepository;
    @Mock
    private ConnectorMessageEvidenceRepository evidenceRepository;
    @Mock
    private ConnectorMessageErrorRepository messageErrorRepository;

    @InjectMocks
    private ConnectorAckMessageTransportStepService service;

    private ConnectorBusinessMessage businessMessage() {
        return BusinessMessageTestFixtures.createInboundMessage()
                                          .toBuilder()
                                          .identifier(MESSAGE_ID)
                                          .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                          .build();
    }

    private ConnectorEvidenceMessage evidenceMessage() {
        return EvidenceMessageTestFixtures.createDeliveryEvidenceMessage()
                                          .toBuilder()
                                          .identifier(MESSAGE_ID)
                                          .build();
    }

    private UpdateMessageTransportCommand deliveredCommand() {
        return UpdateMessageTransportCommand.builder()
                                            .status(ConnectorMessageTransportStatus.DELIVERED)
                                            .remoteMessageIdentifier(BACKEND_ID)
                                            .errors(List.of())
                                            .linkPartnerName(LINK_PARTNER)
                                            .build();
    }

    private UpdateMessageTransportCommand failedCommand(List<ConnectorMessageError> errors) {
        return UpdateMessageTransportCommand.builder()
                                            .status(ConnectorMessageTransportStatus.FAILED)
                                            .errors(errors)
                                            .linkPartnerName(LINK_PARTNER)
                                            .build();
    }

    private UpdateMessageTransportCommand commandWithStatus(
        ConnectorMessageTransportStatus status) {
        return deliveredCommand().toBuilder().status(status).build();
    }

    private ConnectorMessageTransportStep stepWithStatus(
        ConnectorMessageTransportStatus status,
        int numberOfAttempts) {
        var statuses = ConnectorMessageTransportStepStatus.builder()
                                                          .status(status)
                                                          .createdAt(Instant.now())
                                                          .build();
        return ConnectorMessageTransportStep.builder()
                                            .identifier(TRANSPORT_STEP_ID)
                                            .transportedMessageIdentifier(MESSAGE_ID)
                                            .status(status)
                                            .statuses(Set.of(statuses))
                                            .numberOfAttempts(numberOfAttempts)
                                            .transportedMessage(businessMessage())
                                            .linkPartnerName(LINK_PARTNER)
                                            .build();
    }

    @Nested
    @DisplayName("when the input is invalid")
    class WhenInputIsInvalid {
        @Test
        void should_fail_when_the_message_identifier_is_null() {
            assertThatThrownBy(() -> service.execute(null, deliveredCommand()))
                .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(
                transportStepRepository,
                messageRepository,
                messageErrorRepository
            );
        }

        @Test
        void should_fail_when_the_command_is_null() {
            assertThatThrownBy(() -> service.execute(MESSAGE_ID, null))
                .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(
                transportStepRepository,
                messageRepository,
                messageErrorRepository
            );
        }

        @Test
        void should_fail_when_both_arguments_are_null() {
            assertThatThrownBy(() -> service.execute(null, null))
                .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(
                transportStepRepository,
                messageRepository,
                messageErrorRepository
            );
        }

        @Test
        void should_fail_when_the_message_identifier_is_empty() {
            assertThatThrownBy(() -> service.execute("", deliveredCommand()))
                .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(
                transportStepRepository,
                messageRepository,
                messageErrorRepository
            );
        }

        @Test
        void should_fail_when_the_message_identifier_is_blank() {
            assertThatThrownBy(() -> service.execute(" ", deliveredCommand()))
                .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(
                transportStepRepository,
                messageRepository,
                messageErrorRepository
            );
        }

        @Test
        void should_fail_when_the_command_failed_with_null_errors() {
            assertThatThrownBy(() -> service.execute(MESSAGE_ID, failedCommand(null)))
                .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(
                transportStepRepository,
                messageRepository,
                messageErrorRepository
            );
        }

        @Test
        void should_fail_when_the_command_failed_with_empty_errors() {
            assertThatThrownBy(() -> service.execute(MESSAGE_ID, failedCommand(List.of())))
                .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(
                transportStepRepository,
                messageRepository,
                messageErrorRepository
            );
        }

        @Test
        void should_fail_when_the_command_status_is_ready_for_pull() {
            var command = commandWithStatus(ConnectorMessageTransportStatus.READY_FOR_PULL);

            assertThatThrownBy(() -> service.execute(MESSAGE_ID, command))
                .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(
                transportStepRepository,
                messageRepository,
                evidenceRepository,
                messageErrorRepository
            );
        }
    }

    @Nested
    @DisplayName("when the transport step is not found")
    class WhenStepNotFound {
        @Test
        void should_fail_when_no_transport_step_exists_for_the_message() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(null);

            assertThatThrownBy(() -> service.execute(MESSAGE_ID, deliveredCommand()))
                .isInstanceOf(ConnectorMessageTransportStepNotFoundException.class);

            verifyNoInteractions(messageRepository, messageErrorRepository);
        }
    }

    @Nested
    @DisplayName("when the requesting link partner does not own the transport")
    class WhenLinkPartnerNotOwner {
        @Test
        void should_fail_when_the_requesting_link_partner_does_not_own_the_transport_of_the_message() {
            var step = stepWithStatus(ConnectorMessageTransportStatus.DELIVERED, 1);
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(step);

            var command = deliveredCommand().toBuilder().linkPartnerName("alice_backend").build();
            assertThatThrownBy(() -> service.execute(MESSAGE_ID, command))
                .isInstanceOf(ConnectorLinkPartnerNotOwnerException.class);

            verifyNoInteractions(messageRepository, messageErrorRepository);
        }

        @Test
        void should_ignore_the_case_of_the_link_partner_name_when_checking_the_ownership() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(stepWithStatus(ConnectorMessageTransportStatus.DELIVERED, 1));

            var command = deliveredCommand().toBuilder()
                                            .linkPartnerName(LINK_PARTNER.toUpperCase())
                                            .build();

            assertThatNoException().isThrownBy(() -> service.execute(MESSAGE_ID, command));
        }
    }

    @Nested
    @DisplayName("when the step is already in a terminal status")
    class WhenStepIsTerminal {
        @Test
        void should_skip_the_update_when_the_step_is_already_delivered() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(stepWithStatus(ConnectorMessageTransportStatus.DELIVERED, 1));

            assertThatNoException()
                .isThrownBy(() -> service.execute(MESSAGE_ID, deliveredCommand()));

            verifyNoInteractions(messageRepository, evidenceRepository, messageErrorRepository);
            verify(transportStepRepository, never()).update(any(), any(), anyInt());
        }

        @Test
        void should_skip_the_update_when_the_step_has_already_failed() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(stepWithStatus(ConnectorMessageTransportStatus.FAILED, 1));

            assertThatNoException()
                .isThrownBy(() -> service.execute(MESSAGE_ID, deliveredCommand()));

            verifyNoInteractions(messageRepository, evidenceRepository, messageErrorRepository);
            verify(transportStepRepository, never()).update(any(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("when the transported message is inconsistent")
    class WhenTransportedMessageIsInconsistent {
        @Test
        void should_fail_when_the_transported_evidence_uuid_is_null() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(
                    stepWithStatus(ConnectorMessageTransportStatus.SUBMITTED, 1)
                        .toBuilder()
                        .transportedMessage(
                            evidenceMessage().toBuilder()
                                             .transportedEvidences(
                                                 List.of(
                                                     EvidenceTestFixtures.createDeliveryEvidence()
                                                                         .toBuilder()
                                                                         .uuid(null)
                                                                         .build()
                                                 )
                                             )
                                             .build()
                        )
                        .build());

            assertThatThrownBy(() -> service.execute(MESSAGE_ID, deliveredCommand()))
                .isInstanceOf(IllegalStateException.class);

            verify(transportStepRepository).update(eq(TRANSPORT_STEP_ID), any(), anyInt());
            verify(transportStepRepository).update(
                TRANSPORT_STEP_ID,
                ConnectorMessageTransportStatus.DELIVERED,
                1
            );
            verify(messageRepository, never()).findByIdentifier(MESSAGE_ID);
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository, never()).updateBackendIdentifier(MESSAGE_ID, BACKEND_ID);
            verify(evidenceRepository, never()).setDeliveredToLinkPartnerAt(any());

            verifyNoInteractions(messageErrorRepository);
        }
    }

    @Nested
    @DisplayName("when the message is pulled")
    class WhenMessageIsPulled {
        @Test
        void should_update_the_step_and_mark_the_business_message_as_pulled() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(stepWithStatus(ConnectorMessageTransportStatus.READY_FOR_PULL, 0));

            service.execute(
                MESSAGE_ID,
                commandWithStatus(ConnectorMessageTransportStatus.PULLED)
            );

            verify(transportStepRepository).update(eq(TRANSPORT_STEP_ID), any(), anyInt());
            verify(transportStepRepository).update(
                TRANSPORT_STEP_ID,
                ConnectorMessageTransportStatus.PULLED,
                0
            );
            verify(messageRepository).updateStatus(
                MESSAGE_ID,
                ConnectorBusinessMessageStatus.PULLED
            );
            verify(messageRepository, never()).findByIdentifier(MESSAGE_ID);
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository, never()).updateBackendIdentifier(MESSAGE_ID, BACKEND_ID);
            verifyNoInteractions(evidenceRepository, messageErrorRepository);
        }

        @Test
        void should_not_update_any_message_status_when_the_transported_message_is_an_evidence() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(
                    stepWithStatus(ConnectorMessageTransportStatus.READY_FOR_PULL, 0)
                        .toBuilder()
                        .transportedMessage(evidenceMessage())
                        .build());

            service.execute(
                MESSAGE_ID,
                commandWithStatus(ConnectorMessageTransportStatus.PULLED)
            );

            verify(transportStepRepository).update(
                TRANSPORT_STEP_ID,
                ConnectorMessageTransportStatus.PULLED,
                0
            );
            verifyNoInteractions(messageRepository, evidenceRepository, messageErrorRepository);
        }
    }

    @Nested
    @DisplayName("when the status requires no post processing")
    class WhenStatusRequiresNoPostProcessing {
        @Test
        void should_only_update_the_step_when_the_status_is_submitted() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(stepWithStatus(ConnectorMessageTransportStatus.PULLED, 1));

            service.execute(
                MESSAGE_ID,
                commandWithStatus(ConnectorMessageTransportStatus.SUBMITTED)
            );

            verify(transportStepRepository).update(
                TRANSPORT_STEP_ID,
                ConnectorMessageTransportStatus.SUBMITTED,
                1
            );
            verifyNoInteractions(messageRepository, evidenceRepository, messageErrorRepository);
        }
    }

    @Nested
    @DisplayName("when the delivery succeeds")
    class WhenDeliverySucceeds {
        @Test
        void should_update_the_business_message_step_and_the_message() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(stepWithStatus(ConnectorMessageTransportStatus.PULLED, 1));
            when(messageRepository.findByIdentifier(MESSAGE_ID)).thenReturn(businessMessage());

            service.execute(MESSAGE_ID, deliveredCommand());

            verify(transportStepRepository).update(eq(TRANSPORT_STEP_ID), any(), anyInt());
            verify(transportStepRepository).update(
                TRANSPORT_STEP_ID,
                ConnectorMessageTransportStatus.DELIVERED,
                1
            );
            verify(messageRepository).findByIdentifier(MESSAGE_ID);
            verify(messageRepository).updateBackendIdentifier(MESSAGE_ID, BACKEND_ID);
            verify(messageRepository).updateStatus(
                MESSAGE_ID,
                ConnectorBusinessMessageStatus.DELIVERED
            );
            verify(messageRepository).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verifyNoInteractions(messageErrorRepository);
        }

        @Test
        void should_only_flag_the_delivery_when_the_message_is_delivered_to_the_gateway() {
            var gatewayMessage = businessMessage()
                .toBuilder()
                .direction(ConnectorMessageDirection.BACKEND_TO_GATEWAY)
                .build();
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(
                    stepWithStatus(ConnectorMessageTransportStatus.PULLED, 1)
                        .toBuilder()
                        .transportedMessage(gatewayMessage)
                        .build());
            when(messageRepository.findByIdentifier(MESSAGE_ID)).thenReturn(gatewayMessage);

            service.execute(MESSAGE_ID, deliveredCommand());

            verify(transportStepRepository).update(
                TRANSPORT_STEP_ID,
                ConnectorMessageTransportStatus.DELIVERED,
                1
            );
            verify(messageRepository).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository, never()).updateBackendIdentifier(any(), any());
            verify(messageRepository, never()).updateStatus(any(), any());
            verifyNoInteractions(messageErrorRepository);
        }

        @Test
        void should_update_the_evidence_message_step_and_flag_the_evidence() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(
                    stepWithStatus(ConnectorMessageTransportStatus.SUBMITTED, 1)
                        .toBuilder()
                        .transportedMessage(evidenceMessage())
                        .build());

            service.execute(MESSAGE_ID, deliveredCommand());

            verify(transportStepRepository).update(eq(TRANSPORT_STEP_ID), any(), anyInt());
            verify(transportStepRepository).update(
                TRANSPORT_STEP_ID,
                ConnectorMessageTransportStatus.DELIVERED,
                1
            );
            verify(messageRepository, never()).findByIdentifier(MESSAGE_ID);
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository, never()).updateBackendIdentifier(MESSAGE_ID, BACKEND_ID);
            verify(evidenceRepository).setDeliveredToLinkPartnerAt(any());

            verifyNoInteractions(messageErrorRepository);
        }

        @Test
        void should_not_update_the_message_info_when_the_message_does_not_exist() {
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(stepWithStatus(ConnectorMessageTransportStatus.PULLED, 1));
            when(messageRepository.findByIdentifier(MESSAGE_ID)).thenReturn(null);

            assertThatNoException()
                .isThrownBy(() -> service.execute(MESSAGE_ID, deliveredCommand()));

            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(messageRepository, never()).updateBackendIdentifier(any(), any());
            verify(messageRepository, never()).updateStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("when the delivery failed")
    class WhenDeliveryFailed {
        @Test
        void should_update_the_step_save_the_errors_and_mark_the_message_as_failed() {
            var errors = List.of(
                ConnectorMessageError.builder().label("timeout").build(),
                ConnectorMessageError.builder().label("connection refused").build()
            );
            when(transportStepRepository.findByStepOrMessageOrRemoteSystemIdentifier(MESSAGE_ID))
                .thenReturn(stepWithStatus(ConnectorMessageTransportStatus.PULLED, 1));

            service.execute(MESSAGE_ID, failedCommand(errors));

            verify(transportStepRepository).update(eq(TRANSPORT_STEP_ID), any(), anyInt());
            verify(transportStepRepository).update(
                TRANSPORT_STEP_ID,
                ConnectorMessageTransportStatus.FAILED,
                1
            );
            verify(messageErrorRepository).save(MESSAGE_ID, errors);
            verify(messageRepository).updateStatus(
                MESSAGE_ID,
                ConnectorBusinessMessageStatus.FAILED
            );
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(messageRepository, never()).updateBackendIdentifier(any(), any());
        }
    }
}
