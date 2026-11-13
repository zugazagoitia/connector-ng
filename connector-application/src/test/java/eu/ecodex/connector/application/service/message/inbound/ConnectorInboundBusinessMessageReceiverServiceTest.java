/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.AS4PropertiesTestFixtures;
import eu.ecodex.connector.BusinessDomainTestFixtures;
import eu.ecodex.connector.EvidenceTestFixtures;
import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.MessageContentTestFixtures;
import eu.ecodex.connector.application.exception.businessdomain.ConnectorBusinessDomainNotEnabledException;
import eu.ecodex.connector.application.exception.businessdomain.ConnectorBusinessDomainNotFoundException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeInvalidTruststoreException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeNotFoundException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeVerificationException;
import eu.ecodex.connector.application.port.api.businessdomain.ConnectorBusinessDomainVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorBusinessMessageVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorMessageIdGenerator;
import eu.ecodex.connector.application.port.api.message.inbound.ConnectorInboundBusinessMessageCommand;
import eu.ecodex.connector.application.port.api.message.inbound.ConnectorInboundBusinessMessageReceiver;
import eu.ecodex.connector.application.port.api.pmode.ConnectorProcessingModeVerifier;
import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.application.port.spi.ConnectorMessageProcessingConfigurationProvider;
import eu.ecodex.connector.domain.model.ProcessingModeVerificationMode;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.processing.ConnectorMessageProcessingConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorInboundBusinessMessageReceiverService")
public class ConnectorInboundBusinessMessageReceiverServiceTest {
    private static final String MESSAGE_ID =
        "28c86f29-5953-42d5-8336-1a03f7e86951@eu.ecodex.connector";

    @Mock
    private ConnectorBusinessDomainVerifier businessDomainVerifierService;
    @Mock
    private ConnectorProcessingModeVerifier processingModeVerifierService;
    @Mock
    private ConnectorMessageProcessingConfigurationProvider configurationProvider;
    @Mock
    private ConnectorBusinessMessageVerifier messageVerifierService;
    @Mock
    private ConnectorMessageIdGenerator messageIdGeneratorService;
    @Mock
    private ConnectorMessageEventPublisher<ConnectorBusinessMessage> stagingEventPublisher;

    private ConnectorInboundBusinessMessageReceiver inboundBusinessMessageReceiver;

    @BeforeEach
    void setUp() {
        inboundBusinessMessageReceiver = new ConnectorInboundBusinessMessageReceiverService(
            businessDomainVerifierService,
            processingModeVerifierService,
            configurationProvider,
            messageVerifierService,
            messageIdGeneratorService,
            stagingEventPublisher
        );
    }

    private ConnectorInboundBusinessMessageCommand createInboundBusinessMessageCommand() {
        return ConnectorInboundBusinessMessageCommand
            .builder()
            .businessDomainIdentifier(
                BusinessDomainTestFixtures.createDefaultBusinessDomain()
                                          .identifier()
            )
            .gatewayName("default_gateway")
            .as4Properties(AS4PropertiesTestFixtures.defaultAS4Properties().build())
            .businessContent(MessageContentTestFixtures.createContent())
            .attachments(List.of(MessageAttachmentTestFixtures.createAttachment()))
            .transportedEvidences(List.of(EvidenceTestFixtures.createRelayREMMDAcceptanceEvidence()))
            .build();
    }

    private void stubConfiguration(ProcessingModeVerificationMode mode) {
        when(configurationProvider.getConfiguration())
            .thenReturn(
                ConnectorMessageProcessingConfiguration
                    .builder()
                    .inboundMessageVerificationMode(mode)
                    .build()
            );
    }

    @Nested
    @DisplayName("when the input is invalid")
    class WhenInputIsInvalid {
        @Test
        void should_fail_when_the_command_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> inboundBusinessMessageReceiver.execute(null)
            );

            verifyNoInteractions(
                businessDomainVerifierService,
                processingModeVerifierService,
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher
            );
        }
    }

    @Nested
    @DisplayName("when receiving an inbound business message")
    class WhenReceivingAnInboundBusinessMessage {
        @Test
        void should_fail_when_the_business_domain_is_not_found() {
            doThrow(ConnectorBusinessDomainNotFoundException.class)
                .when(businessDomainVerifierService).execute(any());

            var inboundMessageCommand = createInboundBusinessMessageCommand();

            assertThrows(
                ConnectorBusinessDomainNotFoundException.class,
                () -> inboundBusinessMessageReceiver.execute(inboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verifyNoInteractions(
                processingModeVerifierService,
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher
            );
        }

        @Test
        void should_fail_when_the_business_domain_is_not_enabled() {
            doThrow(ConnectorBusinessDomainNotEnabledException.class)
                .when(businessDomainVerifierService).execute(any());

            var inboundMessageCommand = createInboundBusinessMessageCommand();

            assertThrows(
                ConnectorBusinessDomainNotEnabledException.class,
                () -> inboundBusinessMessageReceiver.execute(inboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verifyNoInteractions(
                processingModeVerifierService,
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher
            );
        }

        @Test
        void should_fail_when_the_processing_mode_is_not_found() {
            doThrow(ConnectorProcessingModeNotFoundException.class)
                .when(processingModeVerifierService).execute(any());

            var inboundMessageCommand = createInboundBusinessMessageCommand();

            assertThrows(
                ConnectorProcessingModeNotFoundException.class,
                () -> inboundBusinessMessageReceiver.execute(inboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verifyNoInteractions(
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher
            );
        }

        @Test
        void should_fail_when_the_processing_mode_truststore_is_invalid() {
            doThrow(ConnectorProcessingModeInvalidTruststoreException.class)
                .when(processingModeVerifierService).execute(any());

            var inboundMessageCommand = createInboundBusinessMessageCommand();

            assertThrows(
                ConnectorProcessingModeInvalidTruststoreException.class,
                () -> inboundBusinessMessageReceiver.execute(inboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verifyNoInteractions(
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher
            );
        }

        @Test
        void should_fail_when_the_message_verification_fails() {
            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(ProcessingModeVerificationMode.STRICT);
            doThrow(ConnectorProcessingModeVerificationException.class)
                .when(messageVerifierService).verify(any(), any());

            var inboundMessageCommand = createInboundBusinessMessageCommand();

            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> inboundBusinessMessageReceiver.execute(inboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verify(messageIdGeneratorService).execute();
            verify(configurationProvider).getConfiguration();
            verify(messageVerifierService).verify(any(), eq(ProcessingModeVerificationMode.STRICT));
            verifyNoInteractions(stagingEventPublisher);
        }

        @Test
        void should_propagate_the_failure_when_publishing_to_the_staging_queue_fails() {
            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(ProcessingModeVerificationMode.STRICT);
            doThrow(new IllegalStateException("staging queue unavailable"))
                .when(stagingEventPublisher).publish(any());

            var inboundMessageCommand = createInboundBusinessMessageCommand();

            assertThrows(
                IllegalStateException.class,
                () -> inboundBusinessMessageReceiver.execute(inboundMessageCommand)
            );

            verify(messageVerifierService).verify(any(), eq(ProcessingModeVerificationMode.STRICT));
            verify(stagingEventPublisher).publish(any());
        }

        @Test
        void should_submit_the_message_to_the_staging_queue() {
            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(ProcessingModeVerificationMode.STRICT);

            var inboundMessageCommand = createInboundBusinessMessageCommand();

            inboundBusinessMessageReceiver.execute(inboundMessageCommand);

            var messageCaptor = ArgumentCaptor.forClass(ConnectorBusinessMessage.class);
            verify(stagingEventPublisher).publish(messageCaptor.capture());

            var message = messageCaptor.getValue();
            assertThat(message).isNotNull();
            assertThat(message.identifier()).isEqualTo(MESSAGE_ID);
            assertThat(message.status()).isEqualTo(ConnectorBusinessMessageStatus.CREATED);
            assertThat(message.businessDomainIdentifier())
                .isEqualTo(inboundMessageCommand.businessDomainIdentifier());
            assertThat(message.gatewayName()).isEqualTo(inboundMessageCommand.gatewayName());
            assertThat(message.as4Properties()).isEqualTo(inboundMessageCommand.as4Properties());
            assertThat(message.direction())
                .isEqualTo(ConnectorMessageDirection.GATEWAY_TO_BACKEND);
            assertThat(message.businessContent())
                .isEqualTo(inboundMessageCommand.businessContent());
            assertThat(message.attachments()).isEqualTo(inboundMessageCommand.attachments());
            assertThat(message.transportedEvidences())
                .isEqualTo(inboundMessageCommand.transportedEvidences());

            verify(businessDomainVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(inboundMessageCommand.businessDomainIdentifier());
            verify(messageVerifierService)
                .verify(message, ProcessingModeVerificationMode.STRICT);
        }

        @ParameterizedTest
        @EnumSource(ProcessingModeVerificationMode.class)
        void should_verify_the_message_with_the_configured_inbound_verification_mode(
            ProcessingModeVerificationMode mode) {
            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(mode);

            inboundBusinessMessageReceiver.execute(createInboundBusinessMessageCommand());

            var messageCaptor = ArgumentCaptor.forClass(ConnectorBusinessMessage.class);
            verify(stagingEventPublisher).publish(messageCaptor.capture());
            verify(messageVerifierService).verify(messageCaptor.getValue(), mode);
        }

        @Test
        void should_verify_the_message_before_publishing_it_to_the_staging_queue() {
            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(ProcessingModeVerificationMode.STRICT);

            var inboundMessageCommand = createInboundBusinessMessageCommand();

            inboundBusinessMessageReceiver.execute(inboundMessageCommand);

            var orders = inOrder(
                businessDomainVerifierService,
                processingModeVerifierService,
                messageVerifierService,
                stagingEventPublisher
            );
            orders.verify(businessDomainVerifierService)
                   .execute(inboundMessageCommand.businessDomainIdentifier());
            orders.verify(processingModeVerifierService)
                   .execute(inboundMessageCommand.businessDomainIdentifier());
            orders.verify(messageVerifierService).verify(any(), any());
            orders.verify(stagingEventPublisher).publish(any());
        }
    }
}
