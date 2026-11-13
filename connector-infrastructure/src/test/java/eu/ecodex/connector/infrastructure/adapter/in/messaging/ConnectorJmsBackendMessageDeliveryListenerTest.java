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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessDomainTestFixtures;
import eu.ecodex.connector.EvidenceMessageTestFixtures;
import eu.ecodex.connector.EvidenceTestFixtures;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundEvidenceMessageCommand;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundEvidenceMessageReceiver;
import eu.ecodex.connector.application.port.api.transport.ConnectorRegisterMessageTransportStep;
import eu.ecodex.connector.application.port.spi.link.ConnectorLinkPartnerProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageEvidenceRepository;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.domain.ConnectorDefaults;
import eu.ecodex.connector.domain.model.link.ConnectorLinkMode;
import eu.ecodex.connector.domain.model.link.partner.ConnectorLinkPartner;
import eu.ecodex.connector.domain.model.link.partner.ConnectorLinkPartnerName;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorEvidenceMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageAS4Properties;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorEvidenceType;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.pmode.ConnectorPartyRoleType;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeAction;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeService;
import eu.ecodex.connector.domain.transition.DomibsConnectorAcknowledgementType;
import eu.ecodex.connector.domain.transition.DomibusConnectorBackendDeliveryWebService;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageType;
import eu.ecodex.connector.infrastructure.adapter.in.messaging.outbound.ConnectorJmsBackendMessageDeliveryListener;
import eu.ecodex.connector.infrastructure.adapter.out.soap.ConnectorBackendDeliveryServiceClient;
import eu.ecodex.connector.infrastructure.helper.LegacyMessageHelper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("DataFlowIssue")
@DisplayName("ConnectorJmsBackendMessageDeliveryListener")
@ExtendWith(MockitoExtension.class)
public class ConnectorJmsBackendMessageDeliveryListenerTest {
    private static final String MESSAGE_ID = "msg-001";
    private static final String BACKEND_NAME = "backend_alice";
    private static final String BACKEND_MESSAGE_ID = "backend-message-id";

    @Mock
    private ConnectorMessageEvidenceRepository evidenceRepository;
    @Mock
    private ConnectorRegisterMessageTransportStep registerMessageTransportStep;
    @Mock
    private ConnectorMessageRepository messageRepository;
    @Mock
    private ConnectorBackendDeliveryServiceClient backendServiceClient;
    @Mock
    private DomibusConnectorBackendDeliveryWebService deliveryWebService;
    @Mock
    private ConnectorLinkPartnerProvider linkPartnerProvider;
    @Mock
    private LegacyMessageHelper legacyMessageHelper;
    @Mock
    private ConnectorOutboundEvidenceMessageReceiver outboundEvidenceMessageReceiver;

    @InjectMocks
    private ConnectorJmsBackendMessageDeliveryListener listener;

    private ConnectorMessageAS4Properties as4Properties() {
        return ConnectorMessageAS4Properties
            .builder()
            .service(
                ConnectorProcessingModeService.builder()
                                              .name("EPO")
                                              .type("urn:e-codex:services:")
                                              .build()
            )
            .action(
                ConnectorProcessingModeAction.builder()
                                             .name("Form_A")
                                             .build()
            )
            .fromParty(
                ConnectorProcessingModeParty.builder()
                                            .identifier("BL")
                                            .identifierType(
                                                "urn:oasis:names:tc:ebcore:partyid-type:ecodex"
                                            )
                                            .role("GW")
                                            .roleType(ConnectorPartyRoleType.INITIATOR)
                                            .build()
            )
            .toParty(
                ConnectorProcessingModeParty.builder()
                                            .identifier("RE")
                                            .identifierType(
                                                "urn:oasis:names:tc:ebcore:partyid-type:ecodex"
                                            )
                                            .role("GW")
                                            .roleType(ConnectorPartyRoleType.RESPONDER)
                                            .build()
            )
            .conversationIdentifier("3d5ec775-6602-4bb0-a23c-0311ef8dabc8")
            .ebmsMessageIdentifier("50ef4a19-916f-4e38-bc86-4f85921e6f0a@domibus.eu")
            .referenceToIdentifier(null)
            .originalSender("bob")
            .finalRecipient("alice")
            .build();
    }

    private ConnectorMessageBusinessContent businessContent() {
        return ConnectorMessageBusinessContent
            .builder()
            .uuid(UUID.randomUUID().toString())
            .xmlContent(
                ConnectorMessageAttachment.builder()
                                          .identifier("xml-content-id")
                                          .build()
            )
            .build();
    }

    private ConnectorBusinessMessage inboundMessage() {
        return ConnectorBusinessMessage
            .builder()
            .businessDomainIdentifier(
                BusinessDomainTestFixtures
                    .createDefaultBusinessDomain()
                    .identifier()
            )
            .identifier(MESSAGE_ID)
            .backendName(BACKEND_NAME)
            .backendMessageIdentifier("backend-msg-001")
            .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
            .status(ConnectorBusinessMessageStatus.CREATED)
            .as4Properties(as4Properties())
            .businessContent(businessContent())
            .attachments(List.of())
            .evidences(List.of())
            .transportedEvidences(
                List.of(
                    EvidenceTestFixtures.createSubmissionAcceptanceEvidence()
                )
            )
            .build();
    }

    private ConnectorBusinessMessage triggerBusinessMessage() {
        return inboundMessage();
    }

    private ConnectorEvidenceMessage triggerEvidenceMessage() {
        return EvidenceMessageTestFixtures
            .createSubmissionAcceptanceEvidenceMessage()
            .switchDirection();
    }

    private ConnectorLinkPartner linkPartner() {
        return ConnectorLinkPartner
            .builder()
            .name(ConnectorLinkPartnerName.builder().name(BACKEND_NAME).build())
            .senderMode(ConnectorLinkMode.PUSH)
            .build();
    }

    private ConnectorLinkPartner pullLinkPartner() {
        return linkPartner().toBuilder().senderMode(ConnectorLinkMode.PULL).build();
    }

    private ConnectorLinkPartner testLinkPartner() {
        return linkPartner()
            .toBuilder()
            .name(
                ConnectorLinkPartnerName.builder()
                                        .name(ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME)
                                        .build()
            )
            .build();
    }

    private void stubDelivery(boolean accepted, String backendMessageIdentifier) {
        when(backendServiceClient.createClient(any())).thenReturn(deliveryWebService);
        when(legacyMessageHelper.convertMessage(any()))
            .thenReturn(mock(DomibusConnectorMessageType.class));

        var acknowledgement = mock(DomibsConnectorAcknowledgementType.class);
        when(acknowledgement.isResult()).thenReturn(accepted);
        when(acknowledgement.getMessageId()).thenReturn(backendMessageIdentifier);
        when(deliveryWebService.deliverMessage(any())).thenReturn(acknowledgement);
    }

    @Nested
    @DisplayName("invalid JMS message")
    class InvalidMessages {
        @Test
        void should_reject_null_message() {
            assertThatThrownBy(() -> listener.handle(null))
                .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(
                messageRepository,
                backendServiceClient,
                registerMessageTransportStep,
                linkPartnerProvider,
                evidenceRepository,
                legacyMessageHelper,
                outboundEvidenceMessageReceiver
            );
        }

        @Test
        void should_fail_when_backend_is_unknown() {
            when(linkPartnerProvider.findByName(any())).thenReturn(null);

            assertThatThrownBy(() -> listener.handle(inboundMessage()))
                .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(
                messageRepository,
                backendServiceClient,
                registerMessageTransportStep,
                evidenceRepository,
                legacyMessageHelper,
                outboundEvidenceMessageReceiver
            );
        }

        @Test
        void should_look_up_the_link_partner_by_the_backend_name_of_the_message() {
            when(linkPartnerProvider.findByName(any())).thenReturn(pullLinkPartner());

            listener.handle(inboundMessage());

            verify(linkPartnerProvider).findByName(
                ConnectorLinkPartnerName.builder().name(BACKEND_NAME).build()
            );
        }
    }

    @Nested
    @DisplayName("push delivery")
    class PushDelivery {
        @Test
        void should_deliver_business_message_successfully() {
            ReflectionTestUtils.setField(listener, "autoTriggerDeliveryEvidences", true);
            stubDelivery(true, BACKEND_MESSAGE_ID);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            var message = triggerBusinessMessage();
            listener.handle(message);

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.DELIVERED)
            );
            verify(messageRepository).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository).updateStatus(
                MESSAGE_ID,
                ConnectorBusinessMessageStatus.DELIVERED
            );
            verify(messageRepository).updateBackendIdentifier(MESSAGE_ID, BACKEND_MESSAGE_ID);
            verify(messageRepository, never()).setAsRejected(any());
            verify(evidenceRepository).setDeliveredToLinkPartnerAt(
                message.transportedEvidences().getFirst().uuid()
            );

            var commandCaptor =
                ArgumentCaptor.forClass(ConnectorOutboundEvidenceMessageCommand.class);
            verify(outboundEvidenceMessageReceiver).execute(commandCaptor.capture());
            var command = commandCaptor.getValue();
            assertThat(command.evidenceType()).isEqualTo(ConnectorEvidenceType.DELIVERY);
            assertThat(command.referenceToMessageIdentifier())
                .isEqualTo(message.as4Properties().ebmsMessageIdentifier());
            assertThat(command.backendName()).isEqualTo(message.backendName());
        }

        @Test
        void should_deliver_business_message_without_auto_trigger_delivery_evidences() {
            ReflectionTestUtils.setField(listener, "autoTriggerDeliveryEvidences", false);
            stubDelivery(true, BACKEND_MESSAGE_ID);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            listener.handle(triggerBusinessMessage());

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.DELIVERED)
            );
            verify(messageRepository).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository).updateStatus(
                MESSAGE_ID,
                ConnectorBusinessMessageStatus.DELIVERED
            );
            verify(messageRepository).updateBackendIdentifier(MESSAGE_ID, BACKEND_MESSAGE_ID);
            verify(outboundEvidenceMessageReceiver, never()).execute(any());
        }

        @Test
        void should_deliver_business_message_without_backend_identifier_when_acknowledgement_message_id_is_null() {
            ReflectionTestUtils.setField(listener, "autoTriggerDeliveryEvidences", false);
            stubDelivery(true, null);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            listener.handle(triggerBusinessMessage());

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.DELIVERED)
            );
            verify(messageRepository).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository).updateStatus(
                MESSAGE_ID,
                ConnectorBusinessMessageStatus.DELIVERED
            );
            verify(messageRepository, never()).updateBackendIdentifier(any(), any());
        }

        @Test
        void should_deliver_business_message_when_transported_evidences_is_empty() {
            ReflectionTestUtils.setField(listener, "autoTriggerDeliveryEvidences", false);
            stubDelivery(true, BACKEND_MESSAGE_ID);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            var message = inboundMessage()
                .toBuilder()
                .transportedEvidences(List.of())
                .build();

            listener.handle(message);

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.DELIVERED)
            );
            verify(messageRepository).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository).updateBackendIdentifier(MESSAGE_ID, BACKEND_MESSAGE_ID);
            verify(evidenceRepository, never()).setDeliveredToLinkPartnerAt(any());
        }

        @Test
        void should_deliver_business_message_when_transported_evidences_is_null() {
            ReflectionTestUtils.setField(listener, "autoTriggerDeliveryEvidences", false);
            stubDelivery(true, BACKEND_MESSAGE_ID);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            var message = inboundMessage()
                .toBuilder()
                .transportedEvidences(null)
                .build();

            listener.handle(message);

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.DELIVERED)
            );
            verify(evidenceRepository, never()).setDeliveredToLinkPartnerAt(any());
        }

        @Test
        void should_flag_every_transported_evidence_of_the_business_message_as_delivered() {
            stubDelivery(true, BACKEND_MESSAGE_ID);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            var first = EvidenceTestFixtures.createSubmissionAcceptanceEvidence()
                                            .toBuilder()
                                            .uuid("evidence-uuid-1")
                                            .build();
            var second = EvidenceTestFixtures.createDeliveryEvidence()
                                             .toBuilder()
                                             .uuid("evidence-uuid-2")
                                             .build();
            var message = inboundMessage()
                .toBuilder()
                .transportedEvidences(List.of(first, second))
                .build();

            listener.handle(message);

            verify(evidenceRepository).setDeliveredToLinkPartnerAt("evidence-uuid-1");
            verify(evidenceRepository).setDeliveredToLinkPartnerAt("evidence-uuid-2");
        }

        @Test
        void should_fail_when_business_message_transported_evidence_uuid_is_null() {
            stubDelivery(true, null);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            var evidenceWithNullUuid = EvidenceTestFixtures
                .createSubmissionAcceptanceEvidence()
                .toBuilder()
                .uuid(null)
                .build();
            var message = inboundMessage()
                .toBuilder()
                .transportedEvidences(List.of(evidenceWithNullUuid))
                .build();

            assertThatThrownBy(() -> listener.handle(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The evidence message contains no transported evidence");

            verifyNoInteractions(registerMessageTransportStep);
            verify(evidenceRepository, never()).setDeliveredToLinkPartnerAt(any());
        }

        @Test
        void should_propagate_the_failure_when_triggering_the_delivery_evidence_fails() {
            ReflectionTestUtils.setField(listener, "autoTriggerDeliveryEvidences", true);
            stubDelivery(true, BACKEND_MESSAGE_ID);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());
            doThrow(new IllegalStateException("evidence generation failed"))
                .when(outboundEvidenceMessageReceiver).execute(any());

            assertThatThrownBy(() -> listener.handle(triggerBusinessMessage()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("evidence generation failed");

            verifyNoInteractions(registerMessageTransportStep);
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(messageRepository, never()).updateStatus(any(), any());
        }

        @Test
        void should_deliver_evidence_message_successfully() {
            stubDelivery(true, null);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            var message = triggerEvidenceMessage();
            listener.handle(message);

            verify(evidenceRepository).setDeliveredToLinkPartnerAt(
                message.transportedEvidences().getFirst().uuid()
            );
            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.DELIVERED)
            );
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(messageRepository, never()).updateStatus(any(), any());
            verify(messageRepository, never()).updateBackendIdentifier(any(), any());
            verify(messageRepository, never()).setAsRejected(any());
            verify(outboundEvidenceMessageReceiver, never()).execute(any());
        }

        @Test
        void should_fail_when_evidence_message_transported_evidence_uuid_is_null() {
            stubDelivery(true, null);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            var evidenceWithNullUuid = EvidenceTestFixtures
                .createSubmissionAcceptanceEvidence()
                .toBuilder()
                .uuid(null)
                .build();
            var message = triggerEvidenceMessage()
                .toBuilder()
                .transportedEvidences(List.of(evidenceWithNullUuid))
                .build();

            assertThatThrownBy(() -> listener.handle(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The evidence message contains no transported evidence");

            verifyNoInteractions(registerMessageTransportStep);
        }

        @Test
        void should_mark_business_message_as_rejected_when_backend_rejects_it() {
            stubDelivery(false, null);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            listener.handle(triggerBusinessMessage());

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.FAILED)
            );
            verify(messageRepository).setAsRejected(MESSAGE_ID);
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(messageRepository, never()).updateStatus(any(), any());
            verify(evidenceRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(outboundEvidenceMessageReceiver, never()).execute(any());
        }

        @Test
        void should_mark_evidence_message_as_failed_when_backend_rejects_it() {
            stubDelivery(false, null);
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());

            listener.handle(triggerEvidenceMessage());

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.FAILED)
            );
            verify(messageRepository, never()).setAsRejected(any());
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(evidenceRepository, never()).setDeliveredToLinkPartnerAt(any());
        }

        @Test
        void should_mark_delivery_as_failed_when_backend_submission_throws_exception() {
            when(backendServiceClient.createClient(any())).thenReturn(deliveryWebService);
            when(legacyMessageHelper.convertMessage(any()))
                .thenReturn(mock(DomibusConnectorMessageType.class));
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());
            when(deliveryWebService.deliverMessage(any()))
                .thenThrow(new RuntimeException());

            assertThatNoException()
                .isThrownBy(() -> listener.handle(triggerBusinessMessage()));

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.FAILED)
            );
            verify(evidenceRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(messageRepository, never()).updateBackendIdentifier(any(), any());
            verify(messageRepository, never()).setAsRejected(any());
        }

        @Test
        void should_mark_delivery_as_failed_when_the_backend_client_cannot_be_created() {
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());
            when(backendServiceClient.createClient(any()))
                .thenThrow(new IllegalStateException("no endpoint"));

            assertThatNoException()
                .isThrownBy(() -> listener.handle(triggerBusinessMessage()));

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.FAILED)
            );
            verifyNoInteractions(legacyMessageHelper);
            verifyNoInteractions(messageRepository, evidenceRepository);
        }

        @Test
        void should_mark_delivery_as_failed_when_the_message_conversion_fails() {
            when(linkPartnerProvider.findByName(any())).thenReturn(linkPartner());
            when(backendServiceClient.createClient(any())).thenReturn(deliveryWebService);
            when(legacyMessageHelper.convertMessage(any()))
                .thenThrow(new IllegalArgumentException("cannot convert"));

            assertThatNoException()
                .isThrownBy(() -> listener.handle(triggerBusinessMessage()));

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.FAILED)
            );
            verifyNoInteractions(deliveryWebService, messageRepository, evidenceRepository);
        }
    }

    @Nested
    @DisplayName("pull delivery")
    class PullDelivery {
        @Test
        void should_make_business_message_ready_for_download() {
            when(linkPartnerProvider.findByName(any())).thenReturn(pullLinkPartner());

            listener.handle(triggerBusinessMessage());

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.READY_FOR_PULL)
            );
            verify(messageRepository, never()).setDeliveredToLinkPartnerAt(any());
            verify(messageRepository, never()).updateBackendIdentifier(any(), any());
            verify(messageRepository, never()).updateStatus(any(), any());
            verify(messageRepository, never()).setAsRejected(any());
            verifyNoInteractions(
                backendServiceClient,
                evidenceRepository,
                legacyMessageHelper,
                outboundEvidenceMessageReceiver
            );
        }

        @Test
        void should_make_evidence_message_ready_for_download() {
            when(linkPartnerProvider.findByName(any())).thenReturn(pullLinkPartner());

            listener.handle(triggerEvidenceMessage());

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.READY_FOR_PULL)
            );
            verify(evidenceRepository, never()).setDeliveredToLinkPartnerAt(any());
            verifyNoInteractions(
                backendServiceClient,
                messageRepository,
                legacyMessageHelper,
                outboundEvidenceMessageReceiver
            );
        }
    }

    @Nested
    @DisplayName("test message delivery")
    class TestMessageDelivery {
        @Test
        void should_deliver_test_business_message() {
            ReflectionTestUtils.setField(listener, "autoTriggerDeliveryEvidences", false);
            when(linkPartnerProvider.findByName(any())).thenReturn(testLinkPartner());

            var message = inboundMessage()
                .toBuilder()
                .backendName(ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME)
                .build();

            listener.handle(message);

            var backendIdentifierCaptor = ArgumentCaptor.forClass(String.class);
            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.DELIVERED)
            );
            verify(messageRepository).setDeliveredToLinkPartnerAt(MESSAGE_ID);
            verify(messageRepository).updateStatus(
                MESSAGE_ID,
                ConnectorBusinessMessageStatus.DELIVERED
            );
            verify(messageRepository).updateBackendIdentifier(
                eq(MESSAGE_ID),
                backendIdentifierCaptor.capture()
            );
            verify(evidenceRepository).setDeliveredToLinkPartnerAt(
                message.transportedEvidences().getFirst().uuid()
            );
            verifyNoInteractions(
                backendServiceClient,
                legacyMessageHelper,
                outboundEvidenceMessageReceiver
            );
        }

        @Test
        void should_trigger_the_delivery_evidence_for_a_test_business_message_when_auto_trigger_is_enabled() {
            ReflectionTestUtils.setField(listener, "autoTriggerDeliveryEvidences", true);
            when(linkPartnerProvider.findByName(any())).thenReturn(testLinkPartner());

            var message = inboundMessage()
                .toBuilder()
                .backendName(ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME)
                .build();

            listener.handle(message);

            var commandCaptor =
                ArgumentCaptor.forClass(ConnectorOutboundEvidenceMessageCommand.class);
            verify(outboundEvidenceMessageReceiver).execute(commandCaptor.capture());
            assertThat(commandCaptor.getValue().evidenceType())
                .isEqualTo(ConnectorEvidenceType.DELIVERY);
            assertThat(commandCaptor.getValue().backendName())
                .isEqualTo(ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME);
            verifyNoInteractions(backendServiceClient, legacyMessageHelper);
        }

        @Test
        void should_deliver_test_evidence_message() {
            when(linkPartnerProvider.findByName(any())).thenReturn(testLinkPartner());

            var message = triggerEvidenceMessage()
                .toBuilder()
                .backendName(ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME)
                .build();

            listener.handle(message);

            verify(registerMessageTransportStep).execute(
                any(),
                eq(ConnectorMessageTransportStatus.DELIVERED)
            );
            verify(evidenceRepository).setDeliveredToLinkPartnerAt(
                message.transportedEvidences().getFirst().uuid()
            );
            verifyNoInteractions(
                backendServiceClient,
                legacyMessageHelper,
                messageRepository,
                outboundEvidenceMessageReceiver
            );
        }
    }
}
