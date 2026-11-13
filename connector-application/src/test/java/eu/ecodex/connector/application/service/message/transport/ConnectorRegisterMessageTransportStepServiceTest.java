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
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.EvidenceMessageTestFixtures;
import eu.ecodex.connector.TransportStepFixtures;
import eu.ecodex.connector.application.port.spi.ConnectorMessageProcessingConfigurationProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorEvidenceMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.processing.ConnectorMessageProcessingConfiguration;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
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
@DisplayName("ConnectorRegisterMessageTransportStepService")
public class ConnectorRegisterMessageTransportStepServiceTest {
    private static final String MESSAGE_ID = "msg-001";
    private static final String BACKEND_NAME = "backend-a";
    private static final String GATEWAY_NAME = "gateway-a";
    private static final String TRANSPORT_SUFFIX = "gateway.example.com";

    @Mock
    private ConnectorMessageTransportStepRepository transportStepRepository;
    @Mock
    private ConnectorMessageProcessingConfigurationProvider processingConfigurationProvider;
    @Mock
    private ConnectorMessageRepository messageRepository;

    @InjectMocks
    private ConnectorRegisterMessageTransportStepService service;

    private ConnectorBusinessMessage message() {
        return BusinessMessageTestFixtures.createInboundMessage().toBuilder()
                                          .identifier(MESSAGE_ID)
                                          .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                          .backendName(BACKEND_NAME)
                                          .gatewayName(GATEWAY_NAME)
                                          .build();
    }

    private ConnectorEvidenceMessage evidenceMessage() {
        return EvidenceMessageTestFixtures.createDeliveryEvidenceMessage().toBuilder()
                                          .identifier(MESSAGE_ID)
                                          .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                          .backendName(BACKEND_NAME)
                                          .gatewayName(GATEWAY_NAME)
                                          .build();
    }

    private ConnectorMessageProcessingConfiguration configuration() {
        return ConnectorMessageProcessingConfiguration.builder()
                                                      .transportIdSuffix(TRANSPORT_SUFFIX)
                                                      .build();
    }

    @Nested
    @DisplayName("when registration succeeds")
    class WhenRegistrationSucceeds {
        @Test
        void should_register_the_transport_step() {
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.execute(message(), ConnectorMessageTransportStatus.SUBMITTED);

            assertThat(result.numberOfAttempts()).isEqualTo(1);
            assertThat(result.status()).isEqualTo(ConnectorMessageTransportStatus.SUBMITTED);

            verify(transportStepRepository).save(
                argThat(step ->
                            step.numberOfAttempts() == 1
                                && step.status() == ConnectorMessageTransportStatus.SUBMITTED
                )
            );
        }

        @ParameterizedTest
        @EnumSource(
            value = ConnectorMessageTransportStatus.class,
            names = {"SUBMITTED", "DELIVERED", "FAILED", "READY_FOR_PULL"}
        )
        void should_register_the_transport_step_for_every_allowed_status(
            ConnectorMessageTransportStatus status) {
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.execute(message(), status);

            assertThat(result.status()).isEqualTo(status);
            assertThat(result.numberOfAttempts()).isEqualTo(1);
        }

        @Test
        void should_build_the_identifier_from_a_random_uuid_the_suffix_and_the_link_partner() {
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.execute(message(), ConnectorMessageTransportStatus.SUBMITTED);

            var identifier = result.identifier();
            assertThat(identifier).endsWith("@" + TRANSPORT_SUFFIX + "_" + BACKEND_NAME);
        }

        @Test
        void should_generate_a_different_identifier_for_each_registration() {
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var first = service.execute(message(), ConnectorMessageTransportStatus.SUBMITTED);
            var second = service.execute(message(), ConnectorMessageTransportStatus.SUBMITTED);

            assertThat(first.identifier()).isNotEqualTo(second.identifier());
        }

        @Test
        void should_return_the_step_returned_by_the_repository() {
            var saved = TransportStepFixtures.createTransportStepWithBusinessMessage()
                .toBuilder()
                .transportedMessage(message())
                .build();
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenReturn(saved);

            var result = service.execute(message(), ConnectorMessageTransportStatus.SUBMITTED);

            assertThat(result).isSameAs(saved);
        }
    }

    @Nested
    @DisplayName("when resolving the link partner")
    class WhenResolvingTheLinkPartner {
        @Test
        void should_use_the_backend_name_when_the_message_goes_from_the_gateway_to_the_backend() {
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.execute(message(), ConnectorMessageTransportStatus.SUBMITTED);

            assertThat(result.linkPartnerName()).isEqualTo(BACKEND_NAME);
        }

        @Test
        void should_use_the_gateway_name_when_the_message_goes_from_the_backend_to_the_gateway() {
            var outbound = message().toBuilder()
                                    .direction(ConnectorMessageDirection.BACKEND_TO_GATEWAY)
                                    .build();
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.execute(outbound, ConnectorMessageTransportStatus.SUBMITTED);

            assertThat(result.linkPartnerName()).isEqualTo(GATEWAY_NAME);
            assertThat(result.identifier()).endsWith("_" + GATEWAY_NAME);
        }
    }

    @Nested
    @DisplayName("when the transported message is a business message")
    class WhenTransportedMessageIsABusinessMessage {
        @ParameterizedTest
        @EnumSource(
            value = ConnectorMessageTransportStatus.class,
            names = {"SUBMITTED", "DELIVERED", "FAILED", "READY_FOR_PULL"}
        )
        void should_update_the_message_status_from_the_transport_status(
            ConnectorMessageTransportStatus status) {
            var expectedStatus = ConnectorBusinessMessageStatus.fromTransportStep(status.name());
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.execute(message(), status);

            verify(messageRepository).updateStatus(MESSAGE_ID, expectedStatus);
            assertThat(result.transportedMessage())
                .isInstanceOfSatisfying(
                    ConnectorBusinessMessage.class,
                    transported -> assertThat(transported.status()).isEqualTo(expectedStatus)
                );
        }
    }

    @Nested
    @DisplayName("when the transported message is an evidence message")
    class WhenTransportedMessageIsAnEvidenceMessage {
        @Test
        void should_register_the_step_without_updating_any_message_status() {
            var evidence = evidenceMessage();
            when(processingConfigurationProvider.getConfiguration()).thenReturn(configuration());
            when(transportStepRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.execute(evidence, ConnectorMessageTransportStatus.SUBMITTED);

            assertThat(result.transportedMessage()).isSameAs(evidence);
            assertThat(result.status()).isEqualTo(ConnectorMessageTransportStatus.SUBMITTED);
            verifyNoInteractions(messageRepository);
        }
    }

    @Nested
    @DisplayName("when the input is invalid")
    class WhenInputIsInvalid {
        @Test
        void should_fail_when_the_message_is_null() {
            assertThatThrownBy(() -> service.execute(
                null,
                ConnectorMessageTransportStatus.READY_FOR_PULL
            )).isInstanceOf(NullPointerException.class);

            verifyNoInteractions(
                transportStepRepository,
                processingConfigurationProvider,
                messageRepository
            );
        }

        @Test
        void should_fail_when_the_status_is_null() {
            assertThatThrownBy(() -> service.execute(message(), null))
                .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(
                transportStepRepository,
                processingConfigurationProvider,
                messageRepository
            );
        }

        @ParameterizedTest
        @EnumSource(
            value = ConnectorMessageTransportStatus.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"SUBMITTED", "DELIVERED", "FAILED", "READY_FOR_PULL"}
        )
        void should_fail_when_the_status_is_not_allowed_for_registration(
            ConnectorMessageTransportStatus status) {
            var message = message();

            assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(message, status)
            );

            verifyNoInteractions(
                transportStepRepository,
                processingConfigurationProvider,
                messageRepository
            );
        }
    }
}
