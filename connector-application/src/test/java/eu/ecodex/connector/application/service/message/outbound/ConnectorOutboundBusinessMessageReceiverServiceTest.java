/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.AS4PropertiesTestFixtures;
import eu.ecodex.connector.BusinessDomainTestFixtures;
import eu.ecodex.connector.MessageContentTestFixtures;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.application.exception.businessdomain.ConnectorBusinessDomainNotEnabledException;
import eu.ecodex.connector.application.exception.businessdomain.ConnectorBusinessDomainNotFoundException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeInvalidTruststoreException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeNotFoundException;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeVerificationException;
import eu.ecodex.connector.application.port.api.attachment.ConnectorAttachmentVerifier;
import eu.ecodex.connector.application.port.api.businessdomain.ConnectorBusinessDomainVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorBusinessMessageVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorMessageIdGenerator;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundBusinessMessageCommand;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundBusinessMessageReceiver;
import eu.ecodex.connector.application.port.api.pmode.ConnectorProcessingModeVerifier;
import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.application.port.spi.ConnectorMessageProcessingConfigurationProvider;
import eu.ecodex.connector.domain.model.ProcessingModeVerificationMode;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
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
@DisplayName("ConnectorOutboundBusinessMessageReceiverService")
public class ConnectorOutboundBusinessMessageReceiverServiceTest {
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
    private ConnectorAttachmentVerifier attachmentVerifierService;
    @Mock
    private ConnectorMessageEventPublisher<ConnectorBusinessMessage> stagingEventPublisher;

    private ConnectorOutboundBusinessMessageReceiver outboundBusinessMessageReceiver;

    @BeforeEach
    void setUp() {
        outboundBusinessMessageReceiver = new ConnectorOutboundBusinessMessageReceiverService(
            configurationProvider,
            messageVerifierService,
            stagingEventPublisher,
            messageIdGeneratorService,
            businessDomainVerifierService,
            processingModeVerifierService,
            attachmentVerifierService
        );
    }

    private ConnectorOutboundBusinessMessageCommand createBusinessMessageCommand() {
        return ConnectorOutboundBusinessMessageCommand
            .builder()
            .businessDomainIdentifier(
                BusinessDomainTestFixtures.createDefaultBusinessDomain()
                                          .identifier()
            )
            .businessContent(MessageContentTestFixtures.createContent())
            .backendMessageIdentifier(
                "85964ab5-b04b-4d45-97d1-962b565e22df@connector.ecodex.eu")
            .backendName("default_backend")
            .as4Properties(AS4PropertiesTestFixtures.defaultAS4Properties().build())
            .direction(ConnectorMessageDirection.BACKEND_TO_GATEWAY)
            .build();
    }

    private void stubConfiguration(ProcessingModeVerificationMode mode) {
        when(configurationProvider.getConfiguration())
            .thenReturn(
                ConnectorMessageProcessingConfiguration
                    .builder()
                    .outboundMessageVerificationMode(mode)
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
                () -> outboundBusinessMessageReceiver.execute(null)
            );

            verifyNoInteractions(
                businessDomainVerifierService,
                processingModeVerifierService,
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher,
                attachmentVerifierService
            );
        }
    }

    @Nested
    @DisplayName("when receiving a business message")
    class WhenReceivingABusinessMessage {
        @Test
        void should_fail_when_the_business_domain_is_not_found() {
            doThrow(ConnectorBusinessDomainNotFoundException.class)
                .when(businessDomainVerifierService).execute(any());

            var outboundMessageCommand = createBusinessMessageCommand();

            assertThrows(
                ConnectorBusinessDomainNotFoundException.class,
                () -> outboundBusinessMessageReceiver.execute(outboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verifyNoInteractions(
                processingModeVerifierService,
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher,
                attachmentVerifierService
            );
        }

        @Test
        void should_fail_when_the_business_domain_is_not_enabled() {
            doThrow(ConnectorBusinessDomainNotEnabledException.class)
                .when(businessDomainVerifierService).execute(any());

            var outboundMessageCommand = createBusinessMessageCommand();

            assertThrows(
                ConnectorBusinessDomainNotEnabledException.class,
                () -> outboundBusinessMessageReceiver.execute(outboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verifyNoInteractions(
                processingModeVerifierService,
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher,
                attachmentVerifierService
            );
        }

        @Test
        void should_fail_when_the_processing_mode_is_not_found() {
            doThrow(ConnectorProcessingModeNotFoundException.class)
                .when(processingModeVerifierService).execute(any());

            var outboundMessageCommand = createBusinessMessageCommand();

            assertThrows(
                ConnectorProcessingModeNotFoundException.class,
                () -> outboundBusinessMessageReceiver.execute(outboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verifyNoInteractions(
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher,
                attachmentVerifierService
            );
        }

        @Test
        void should_fail_when_the_processing_mode_truststore_is_invalid() {
            doThrow(ConnectorProcessingModeInvalidTruststoreException.class)
                .when(processingModeVerifierService).execute(any());

            var outboundMessageCommand = createBusinessMessageCommand();

            assertThrows(
                ConnectorProcessingModeInvalidTruststoreException.class,
                () -> outboundBusinessMessageReceiver.execute(outboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verifyNoInteractions(
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher,
                attachmentVerifierService
            );
        }

        @Test
        void should_fail_when_attachments_are_invalid() {
            doNothing().when(businessDomainVerifierService).execute(any());
            doNothing().when(processingModeVerifierService).execute(any());
            doThrow(ConnectorMessageAttachmentNotFoundException.class)
                .when(attachmentVerifierService).execute(any());

            var outboundMessageCommand = createBusinessMessageCommand();
            var outboundMessageCommandWithAttachment = outboundMessageCommand.toBuilder()
                .attachments(List.of(ConnectorMessageAttachment.builder()
                    .identifier("identifier")
                    .build()))
                .build();


            assertThrows(
                ConnectorMessageAttachmentNotFoundException.class,
                () -> outboundBusinessMessageReceiver.execute(outboundMessageCommandWithAttachment)
            );

            verify(businessDomainVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verify(attachmentVerifierService)
                .execute(outboundMessageCommandWithAttachment.attachments());
            verifyNoInteractions(
                configurationProvider,
                messageVerifierService,
                messageIdGeneratorService,
                stagingEventPublisher
            );
        }

        @Test
        void should_fail_when_the_message_verification_fails() {
            doNothing().when(businessDomainVerifierService).execute(any());
            doNothing().when(processingModeVerifierService).execute(any());
            doNothing().when(attachmentVerifierService).execute(any());

            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(ProcessingModeVerificationMode.STRICT);
            doThrow(ConnectorProcessingModeVerificationException.class)
                .when(messageVerifierService).verify(any(), any());

            var outboundMessageCommand = createBusinessMessageCommand();

            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> outboundBusinessMessageReceiver.execute(outboundMessageCommand)
            );

            verify(businessDomainVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
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

            var outboundMessageCommand = createBusinessMessageCommand();

            assertThrows(
                IllegalStateException.class,
                () -> outboundBusinessMessageReceiver.execute(outboundMessageCommand)
            );

            verify(messageVerifierService).verify(any(), eq(ProcessingModeVerificationMode.STRICT));
            verify(stagingEventPublisher).publish(any());
        }

        @Test
        void should_submit_the_message_to_the_staging_queue() {
            doNothing().when(businessDomainVerifierService).execute(any());
            doNothing().when(processingModeVerifierService).execute(any());
            doNothing().when(attachmentVerifierService).execute(any());

            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(ProcessingModeVerificationMode.STRICT);

            var outboundMessageCommand = createBusinessMessageCommand();

            var result = outboundBusinessMessageReceiver.execute(outboundMessageCommand);

            var messageCaptor = ArgumentCaptor.forClass(ConnectorBusinessMessage.class);
            verify(stagingEventPublisher).publish(messageCaptor.capture());

            var message = messageCaptor.getValue();
            assertThat(message).isNotNull();
            assertThat(message.identifier()).isEqualTo(MESSAGE_ID);
            assertThat(message.status()).isEqualTo(ConnectorBusinessMessageStatus.CREATED);
            assertThat(message.businessDomainIdentifier())
                .isEqualTo(outboundMessageCommand.businessDomainIdentifier());
            assertThat(message.backendMessageIdentifier())
                .isEqualTo(outboundMessageCommand.backendMessageIdentifier());
            assertThat(message.backendName()).isEqualTo(outboundMessageCommand.backendName());
            assertThat(message.as4Properties()).isEqualTo(outboundMessageCommand.as4Properties());
            assertThat(message.direction())
                .isEqualTo(ConnectorMessageDirection.BACKEND_TO_GATEWAY);
            assertThat(message.businessContent())
                .isEqualTo(outboundMessageCommand.businessContent());
            assertThat(message.attachments()).isEqualTo(outboundMessageCommand.attachments());

            assertThat(result).isSameAs(message);

            verify(businessDomainVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verify(processingModeVerifierService)
                .execute(outboundMessageCommand.businessDomainIdentifier());
            verify(messageVerifierService)
                .verify(message, ProcessingModeVerificationMode.STRICT);
            verify(attachmentVerifierService)
                .execute(outboundMessageCommand.attachments());
        }

        @ParameterizedTest
        @EnumSource(ProcessingModeVerificationMode.class)
        void should_verify_the_message_with_the_configured_outbound_verification_mode(
            ProcessingModeVerificationMode mode) {
            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(mode);

            var result = outboundBusinessMessageReceiver.execute(createBusinessMessageCommand());

            verify(messageVerifierService).verify(result, mode);
            verify(stagingEventPublisher).publish(result);
        }

        @ParameterizedTest
        @EnumSource(ConnectorMessageDirection.class)
        void should_keep_the_direction_of_the_command(ConnectorMessageDirection direction) {
            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(ProcessingModeVerificationMode.STRICT);
            var command = createBusinessMessageCommand().toBuilder()
                                                        .direction(direction)
                                                        .build();

            var result = outboundBusinessMessageReceiver.execute(command);

            assertThat(result.direction()).isEqualTo(direction);
        }

        @Test
        void should_verify_the_message_before_publishing_it_to_the_staging_queue() {
            when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
            stubConfiguration(ProcessingModeVerificationMode.STRICT);

            var outboundMessageCommand = createBusinessMessageCommand();

            outboundBusinessMessageReceiver.execute(outboundMessageCommand);

            var orders = inOrder(
                businessDomainVerifierService,
                processingModeVerifierService,
                messageVerifierService,
                stagingEventPublisher
            );
            orders.verify(businessDomainVerifierService)
                   .execute(outboundMessageCommand.businessDomainIdentifier());
            orders.verify(processingModeVerifierService)
                   .execute(outboundMessageCommand.businessDomainIdentifier());
            orders.verify(messageVerifierService).verify(any(), any());
            orders.verify(stagingEventPublisher).publish(any());
        }
    }
}
