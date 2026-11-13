/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message.c2ctest;


import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessDomainIdentifierTestFixtures;
import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.application.exception.c2ctest.ConnectorC2CTestMessageDisabledException;
import eu.ecodex.connector.application.port.api.attachment.ConnectorAttachmentVerifier;
import eu.ecodex.connector.application.port.api.businessdomain.ConnectorBusinessDomainVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorBusinessMessageVerifier;
import eu.ecodex.connector.application.port.api.message.ConnectorMessageIdGenerator;
import eu.ecodex.connector.application.port.api.message.c2ctest.ConnectorTestBusinessMessageAS4PropertiesCommand;
import eu.ecodex.connector.application.port.api.message.c2ctest.ConnectorTestBusinessMessageCommand;
import eu.ecodex.connector.application.port.api.pmode.ConnectorProcessingModeVerifier;
import eu.ecodex.connector.application.port.spi.ConnectorMessageEventPublisher;
import eu.ecodex.connector.application.port.spi.ConnectorMessageProcessingConfigurationProvider;
import eu.ecodex.connector.application.port.spi.ConnectorTestMessageConfigurationProvider;
import eu.ecodex.connector.domain.ConnectorDefaults;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.pmode.ConnectorPartyRoleType;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorSendOutboundC2CTestMessageService")
class ConnectorSendOutboundC2CTestMessageServiceTest {
    private static final String MESSAGE_ID =
        "223caef9-cae9-4387-a38c-ad4879f94b4e@connector.ecodex.eu";
    private static final String BACKEND_MESSAGE_IDENTIFIER =
        "56ed1a8f-b089-4615-9ddd-da302a665a11@backend_system";
    private static final String EBMS_IDENTIFIER = "ebms-id-001@connector.ecodex.eu";
    private static final String CONVERSATION_IDENTIFIER = "e6a173ec-de21-46dc-8a19-63a6cb74915d";
    private static final String PARTY_IDENTIFIER_TYPE =
        "urn:oasis:names:tc:ebcore:partyid-type:ecodex";
    private static final String TEST_SERVICE_NAME = "Connector-TEST";
    private static final String TEST_SERVICE_TYPE = "urn:e-codex:services:";
    private static final String TEST_ACTION = "ConTest_Form";
    private final ConnectorBusinessDomainIdentifier businessDomainIdentifier =
        BusinessDomainIdentifierTestFixtures.createDefaultBusinessDomainIdentifier();
    private final ConnectorMessageBusinessContent businessContent =
        BusinessMessageTestFixtures.createOutboundMessage().businessContent();
    private final List<ConnectorMessageAttachment> attachments =
        List.of(MessageAttachmentTestFixtures.createAttachment());
    private final ConnectorProcessingModeParty fromParty = ConnectorProcessingModeParty
        .builder()
        .identifier("BL")
        .identifierType(PARTY_IDENTIFIER_TYPE)
        .role("GW")
        .roleType(ConnectorPartyRoleType.INITIATOR)
        .build();
    private final ConnectorProcessingModeParty toParty = ConnectorProcessingModeParty
        .builder()
        .identifier("RE")
        .identifierType(PARTY_IDENTIFIER_TYPE)
        .role("GW")
        .roleType(ConnectorPartyRoleType.RESPONDER)
        .build();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConnectorTestMessageConfigurationProvider testMessageConfigProvider;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConnectorMessageProcessingConfigurationProvider messageProcessingConfigProvider;
    @Mock
    private ConnectorMessageIdGenerator messageIdGeneratorService;
    @Mock
    private ConnectorBusinessDomainVerifier businessDomainVerifierService;
    @Mock
    private ConnectorProcessingModeVerifier processingModeVerifierService;
    @Mock
    private ConnectorBusinessMessageVerifier messageVerifierService;
    @Mock
    private ConnectorMessageEventPublisher<ConnectorBusinessMessage> stagingEventPublisher;
    @Mock
    private ConnectorAttachmentVerifier attachmentVerifierService;

    private ConnectorSendOutboundC2CTestMessageService service;

    @BeforeEach
    void setUp() {
        service = new ConnectorSendOutboundC2CTestMessageService(
            testMessageConfigProvider,
            messageProcessingConfigProvider,
            messageIdGeneratorService,
            businessDomainVerifierService,
            processingModeVerifierService,
            messageVerifierService,
            stagingEventPublisher,
            attachmentVerifierService
        );
    }

    private ConnectorTestBusinessMessageCommand createCommand() {
        var as4PropertiesCommand = ConnectorTestBusinessMessageAS4PropertiesCommand
            .builder()
            .ebmsIdentifier(EBMS_IDENTIFIER)
            .conversationIdentifier(CONVERSATION_IDENTIFIER)
            .originalSender("alice")
            .finalRecipient("bob")
            .fromParty(fromParty)
            .toParty(toParty)
            .build();

        return ConnectorTestBusinessMessageCommand
            .builder()
            .businessDomainIdentifier(businessDomainIdentifier)
            .backendMessageIdentifier(BACKEND_MESSAGE_IDENTIFIER)
            .as4PropertiesCommand(as4PropertiesCommand)
            .businessContent(businessContent)
            .attachments(attachments)
            .build();
    }

    private void stubTestMessagesEnabled(boolean enabled) {
        when(testMessageConfigProvider.getConfig().enabled()).thenReturn(enabled);
    }

    /**
     * Stubs everything the service reads once the verifications have passed.
     */
    private void stubMessageCreation() {
        when(testMessageConfigProvider.getConfig().action()).thenReturn(TEST_ACTION);
        when(testMessageConfigProvider.getConfig().service().name())
            .thenReturn(TEST_SERVICE_NAME);
        when(testMessageConfigProvider.getConfig().service().type())
            .thenReturn(TEST_SERVICE_TYPE);
        when(messageIdGeneratorService.execute()).thenReturn(MESSAGE_ID);
    }

    @Nested
    @DisplayName("when the command is invalid")
    class WhenCommandIsInvalid {
        @Test
        void should_fail_when_the_command_is_null() {
            assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("command");

            verifyNoInteractions(
                testMessageConfigProvider,
                businessDomainVerifierService,
                processingModeVerifierService,
                attachmentVerifierService,
                messageIdGeneratorService,
                messageVerifierService,
                stagingEventPublisher
            );
        }
    }

    @Nested
    @DisplayName("when test messages are disabled")
    class WhenTestMessagesAreDisabled {
        @Test
        void should_fail_when_test_messages_are_disabled() {
            stubTestMessagesEnabled(false);

            assertThatThrownBy(() -> service.execute(createCommand()))
                .isInstanceOf(ConnectorC2CTestMessageDisabledException.class)
                .hasMessage("C2C Test message is disabled");

            verifyNoInteractions(
                businessDomainVerifierService,
                processingModeVerifierService,
                attachmentVerifierService,
                messageIdGeneratorService,
                messageVerifierService,
                stagingEventPublisher
            );
        }
    }

    @Nested
    @DisplayName("when test messages are enabled")
    class WhenTestMessagesAreEnabled {
        @BeforeEach
        void setUp() {
            stubTestMessagesEnabled(true);
            stubMessageCreation();
        }

        @Test
        void should_submit_message_to_the_staging_queue_when_valid() {
            var message = service.execute(createCommand());

            assertThat(message.identifier()).isEqualTo(MESSAGE_ID);
            assertThat(message.businessDomainIdentifier()).isEqualTo(businessDomainIdentifier);
            assertThat(message.backendMessageIdentifier()).isEqualTo(BACKEND_MESSAGE_IDENTIFIER);
            assertThat(message.backendName())
                .isEqualTo(ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME);
            assertThat(message.direction())
                .isEqualTo(ConnectorMessageDirection.BACKEND_TO_GATEWAY);
            assertThat(message.businessContent()).isEqualTo(businessContent);
            assertThat(message.attachments()).isEqualTo(attachments);

            verify(stagingEventPublisher).publish(message);
        }

        @Test
        void should_verify_the_business_domain_the_processing_mode_and_the_attachments() {
            service.execute(createCommand());

            verify(businessDomainVerifierService).execute(businessDomainIdentifier);
            verify(processingModeVerifierService).execute(businessDomainIdentifier);
            verify(attachmentVerifierService).execute(attachments);
        }
    }

    @Nested
    @DisplayName("when a verification fails")
    class WhenAVerificationFails {
        @BeforeEach
        void setUp() {
            stubTestMessagesEnabled(true);
        }

        @Test
        void should_propagate_and_publish_nothing_when_the_business_domain_is_invalid() {
            var failure = new IllegalStateException("unknown business domain");
            doThrow(failure).when(businessDomainVerifierService).execute(any());

            assertThatThrownBy(() -> service.execute(createCommand())).isSameAs(failure);

            verifyNoInteractions(
                processingModeVerifierService,
                attachmentVerifierService,
                messageIdGeneratorService,
                messageVerifierService,
                stagingEventPublisher
            );
        }

        @Test
        void should_propagate_and_publish_nothing_when_the_processing_mode_is_invalid() {
            var failure = new IllegalStateException("no processing mode");
            doThrow(failure).when(processingModeVerifierService).execute(any());

            assertThatThrownBy(() -> service.execute(createCommand())).isSameAs(failure);

            verifyNoInteractions(
                attachmentVerifierService,
                messageIdGeneratorService,
                messageVerifierService,
                stagingEventPublisher
            );
        }

        @Test
        void should_propagate_and_publish_nothing_when_the_attachments_are_invalid() {
            var failure = new IllegalStateException("invalid attachments");
            doThrow(failure).when(attachmentVerifierService).execute(any());

            assertThatThrownBy(() -> service.execute(createCommand())).isSameAs(failure);

            verifyNoInteractions(
                messageIdGeneratorService,
                messageVerifierService,
                stagingEventPublisher
            );
        }

        @Test
        void should_propagate_and_publish_nothing_when_the_message_is_invalid() {
            stubMessageCreation();
            var failure = new IllegalStateException("invalid message");
            doThrow(failure).when(messageVerifierService).verify(any(), any());

            assertThatThrownBy(() -> service.execute(createCommand())).isSameAs(failure);

            verify(stagingEventPublisher, never()).publish(any());
        }
    }
}
