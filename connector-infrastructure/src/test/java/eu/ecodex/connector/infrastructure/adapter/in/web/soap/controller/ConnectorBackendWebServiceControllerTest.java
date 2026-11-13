/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.soap.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.SoapMessageSubmitTestFixtures;
import eu.ecodex.connector.TransportStepFixtures;
import eu.ecodex.connector.application.exception.NotFoundException;
import eu.ecodex.connector.application.exception.message.ConnectorLinkPartnerNotOwnerException;
import eu.ecodex.connector.application.port.api.attachment.ConnectorUploadAttachments;
import eu.ecodex.connector.application.port.api.attachment.FileUploadCommand;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundBusinessMessageReceiver;
import eu.ecodex.connector.application.port.api.message.outbound.ConnectorOutboundEvidenceMessageReceiver;
import eu.ecodex.connector.application.port.api.transport.ConnectorAckMessageTransportStep;
import eu.ecodex.connector.application.port.api.transport.ConnectorListPendingTransportStepsIds;
import eu.ecodex.connector.application.port.api.transport.ConnectorRetrieveMessageByTransportId;
import eu.ecodex.connector.application.port.api.transport.ConnectorSetMessagesTransportStepToPull;
import eu.ecodex.connector.application.service.message.transport.ConnectorListPendingTransportStepsService;
import eu.ecodex.connector.domain.transition.DomibusConnectorBackendWebService;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageResponseType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageType;
import eu.ecodex.connector.domain.transition.EmptyRequestType;
import eu.ecodex.connector.domain.transition.GetMessageByIdRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.ConnectorBackendClientVerifier;
import eu.ecodex.connector.infrastructure.adapter.in.web.soap.helper.ConnectorSOAPMessageAttachmentHelpers;
import eu.ecodex.connector.infrastructure.adapter.in.web.soap.interceptor.ProcessMessageAfterDownload;
import eu.ecodex.connector.infrastructure.adapter.in.web.soap.interceptor.ProcessMessagesAfterDownload;
import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import eu.ecodex.connector.infrastructure.helper.ConnectorTempFilesHelper;
import eu.ecodex.connector.infrastructure.helper.LegacyMessageHelper;
import eu.ecodex.connector.link.LinkPartnerTestFixtures;
import jakarta.xml.ws.WebServiceContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorBackendWebServiceController")
public class ConnectorBackendWebServiceControllerTest {
    private static final String TRANSPORT_ID =
        "2921bed4-5587-488b-93a2-c048bc130a12@connector.ecodex.eu_backend_alice";

    @TempDir
    Path uploadDirectory;

    @Mock
    private ConnectorOutboundBusinessMessageReceiver businessMessageReceiver;
    @Mock
    private ConnectorOutboundEvidenceMessageReceiver outboundEvidenceMessageProcessor;
    @Mock
    private ConnectorListPendingTransportStepsIds listPendingMessageIds;
    @Mock
    private ConnectorListPendingTransportStepsService listPendingTransportStepsService;
    @Mock
    private ConnectorRetrieveMessageByTransportId retrieveMessageByTransportId;
    @Mock
    private ConnectorUploadAttachments uploadAttachments;
    @Mock
    private ConnectorBackendClientVerifier backendClientVerifier;
    @Mock
    private ConnectorSetMessagesTransportStepToPull changePendingMessagesStatus;
    @Mock
    private ConnectorAckMessageTransportStep acknowledgeMessageTransportStep;
    @Mock
    private LegacyMessageHelper legacyMessageHelper;
    @Mock
    private WrappedMessageContext wrappedMessageContext;
    @Mock
    private Message cxfMessage;
    @Mock
    private InterceptorChain interceptorChain;
    @Mock
    private WebServiceContext webServiceContext;

    private ConnectorSOAPMessageAttachmentHelpers attachmentHelpers;
    private DomibusConnectorBackendWebService backendWebService;

    @BeforeEach
    void setUp() {
        attachmentHelpers = new ConnectorSOAPMessageAttachmentHelpers(
            new ConnectorTempFilesHelper(new ConnectorFileStorageProperties(uploadDirectory, null))
        );

        backendWebService = new ConnectorBackendWebServiceController(
            businessMessageReceiver,
            outboundEvidenceMessageProcessor,
            listPendingMessageIds,
            listPendingTransportStepsService,
            retrieveMessageByTransportId,
            uploadAttachments,
            changePendingMessagesStatus,
            acknowledgeMessageTransportStep,
            backendClientVerifier,
            legacyMessageHelper,
            attachmentHelpers
        );
        // @Resource field isn't constructor-injected, so set it manually
        ReflectionTestUtils.setField(backendWebService, "webServiceContext", webServiceContext);
    }

    private List<Path> remainingTempFiles() throws IOException {
        try (var files = Files.list(uploadDirectory)) {
            return files.toList();
        }
    }

    private void stubAuthenticatedBackendClient() {
        when(webServiceContext.getUserPrincipal()).thenReturn((UserPrincipal) () -> "CN=alice");
        when(backendClientVerifier.getBackendClient(any()))
            .thenReturn(LinkPartnerTestFixtures.createAliceBackendLinkPartner().name().name());
    }

    private DomibusConnectorMessageResponseType acknowledgeMessage(boolean result) {
        var ackResponse = new DomibusConnectorMessageResponseType();
        ackResponse.setResult(result);
        ackResponse.setAssignedMessageId("12345678-1234-1234-1234-123456789012");
        ackResponse.setResponseForMessageId(
            "3fae4358-7cc9-4929-a17b-4432cbb8b9cc@connector.ecodex.eu");
        ackResponse.setResultMessage("Message acknowledged successfully");
        return ackResponse;
    }

    @Nested
    @DisplayName("submitMessage (backend -> connector)")
    class SubmitMessage {
        @Test
        void should_return_a_successful_ack() {
            stubAuthenticatedBackendClient();
            when(uploadAttachments.execute(any()))
                .thenReturn(List.of(MessageAttachmentTestFixtures.createAttachment()));
            when(businessMessageReceiver.execute(any()))
                .thenReturn(BusinessMessageTestFixtures.createOutboundMessage());

            var payload = SoapMessageSubmitTestFixtures.createBackendToConnectorMessage();
            var ack = backendWebService.submitMessage(payload);

            assertThat(ack).isNotNull();
            assertThat(ack.getMessageId())
                .isEqualTo("223caef9-cae9-4387-a38c-ad4879f94b4e@connector.ecodex.eu");
            assertThat(ack.getResultMessage()).isNullOrEmpty();
            assertThat(ack.isResult()).isTrue();
        }

        @Test
        void should_return_a_failure_ack_when_an_exception_occurs() {
            stubAuthenticatedBackendClient();
            when(uploadAttachments.execute(any()))
                .thenReturn(List.of(MessageAttachmentTestFixtures.createAttachment()));
            when(businessMessageReceiver.execute(any()))
                .thenThrow(new RuntimeException("Error"));

            var payload =
                SoapMessageSubmitTestFixtures.createBackendToConnectorMessageWithoutAttachment();
            var ack = backendWebService.submitMessage(payload);

            assertThat(ack).isNotNull();
            assertThat(ack.getMessageId()).isEqualTo("0093ac10-447c-4946-89a8-dcd9e7a2f4ab");
            assertThat(ack.getResultMessage()).isNotNull();
            assertThat(ack.isResult()).isFalse();
        }
    }

    @Nested
    @DisplayName("listPendingMessageIds")
    class ListPendingMessageIds {
        @Test
        void should_return_the_pending_message_ids() {
            stubAuthenticatedBackendClient();
            when(listPendingMessageIds.execute(any())).thenReturn(List.of(TRANSPORT_ID));

            var response = backendWebService.listPendingMessageIds(new EmptyRequestType());

            assertThat(response).isNotNull();
            assertThat(response.getMessageTransportIds()).containsExactly(TRANSPORT_ID);
        }
    }

    @Nested
    @DisplayName("getMessageById")
    class GetMessageById {
        private static final String LINK_PARTNER = "backend_alice";

        @Test
        void should_return_the_message() {
            var connectorMessage = BusinessMessageTestFixtures.createInboundMessage();

            stubAuthenticatedBackendClient();
            when(retrieveMessageByTransportId.execute(TRANSPORT_ID, LINK_PARTNER))
                .thenReturn(connectorMessage);
            when(webServiceContext.getMessageContext()).thenReturn(wrappedMessageContext);
            when(wrappedMessageContext.getWrappedMessage()).thenReturn(cxfMessage);
            when(cxfMessage.getInterceptorChain()).thenReturn(interceptorChain);

            var expectedResult = new DomibusConnectorMessageType();
            when(legacyMessageHelper.convertMessage(connectorMessage)).thenReturn(expectedResult);

            var request = new GetMessageByIdRequest();
            request.setMessageTransportId(TRANSPORT_ID);

            var response = backendWebService.getMessageById(request);

            assertThat(response).isNotNull();
            verify(retrieveMessageByTransportId).execute(TRANSPORT_ID, LINK_PARTNER);
            verify(legacyMessageHelper).convertMessage(connectorMessage);

            var interceptorCaptor = ArgumentCaptor.forClass(ProcessMessageAfterDownload.class);
            verify(interceptorChain).add(interceptorCaptor.capture());
            assertThat(interceptorCaptor.getValue()).isInstanceOf(ProcessMessageAfterDownload.class);
        }

        @Test
        void should_fail_when_the_message_is_not_found() {
            var request = new GetMessageByIdRequest();
            request.setMessageTransportId("UNKNOWN-ID");

            stubAuthenticatedBackendClient();
            doThrow(NotFoundException.class)
                .when(retrieveMessageByTransportId).execute("UNKNOWN-ID", LINK_PARTNER);

            assertThatThrownBy(() -> backendWebService.getMessageById(request))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void should_fail_when_the_backend_link_partner_does_not_own_the_message() {
            var request = new GetMessageByIdRequest();
            request.setMessageTransportId(TRANSPORT_ID);

            stubAuthenticatedBackendClient();

            doThrow(ConnectorLinkPartnerNotOwnerException.class)
                .when(retrieveMessageByTransportId).execute(TRANSPORT_ID, LINK_PARTNER);

            assertThatThrownBy(() -> backendWebService.getMessageById(request))
                .isInstanceOf(ConnectorLinkPartnerNotOwnerException.class);
        }
    }

    @Nested
    @DisplayName("requestMessages (list pending messages)")
    class RequestMessages {
        @Test
        void should_return_the_pending_messages() {
            var transportStep = TransportStepFixtures.createTransportStepWithBusinessMessage();

            when(listPendingTransportStepsService.execute("backend_alice"))
                .thenReturn(List.of(transportStep));
            stubAuthenticatedBackendClient();
            when(webServiceContext.getMessageContext()).thenReturn(wrappedMessageContext);
            when(wrappedMessageContext.getWrappedMessage()).thenReturn(cxfMessage);
            when(cxfMessage.getInterceptorChain()).thenReturn(interceptorChain);

            var expectedResult = new DomibusConnectorMessageType();
            var connectorMessage = transportStep.transportedMessage();
            when(legacyMessageHelper.convertMessage(connectorMessage)).thenReturn(expectedResult);

            var response = backendWebService.requestMessages(new EmptyRequestType());

            assertThat(response).isNotNull();
            verify(listPendingTransportStepsService).execute("backend_alice");
            verify(legacyMessageHelper).convertMessage(connectorMessage);

            var interceptorCaptor = ArgumentCaptor.forClass(ProcessMessagesAfterDownload.class);
            verify(interceptorChain).add(interceptorCaptor.capture());
            assertThat(interceptorCaptor.getValue())
                .isInstanceOf(ProcessMessagesAfterDownload.class);
        }
    }

    @Nested
    @DisplayName("acknowledgeMessage")
    class AcknowledgeMessage {
        @Test
        void should_acknowledge_a_message_with_success() {
            stubAuthenticatedBackendClient();
            doNothing().when(acknowledgeMessageTransportStep).execute(any(), any());

            var response = backendWebService.acknowledgeMessage(acknowledgeMessage(true));

            assertThat(response).isNotNull();
        }

        @Test
        void should_acknowledge_a_message_with_failure() {
            stubAuthenticatedBackendClient();
            doNothing().when(acknowledgeMessageTransportStep).execute(any(), any());

            var response = backendWebService.acknowledgeMessage(acknowledgeMessage(false));

            assertThat(response).isNotNull();
        }

        @Test
        void should_failed_when_the_backend_link_partner_does_not_own_the_message() {
            stubAuthenticatedBackendClient();
            doThrow(ConnectorLinkPartnerNotOwnerException.class)
                .when(acknowledgeMessageTransportStep).execute(any(), any());

            assertThatThrownBy(() -> backendWebService.acknowledgeMessage(acknowledgeMessage(true)))
                .isInstanceOf(ConnectorLinkPartnerNotOwnerException.class);
        }
    }

    @Nested
    @DisplayName("temp files")
    class TempFiles {
        @BeforeEach
        void stubCollaborators() {
            // lenient: the rejection test overrides businessMessageReceiver, and strict
            // stubs would report the original stubbing as unused
            lenient().when(webServiceContext.getUserPrincipal())
                     .thenReturn((UserPrincipal) () -> "CN=alice");
            lenient().when(backendClientVerifier.getBackendClient(any()))
                     .thenReturn(LinkPartnerTestFixtures.createAliceBackendLinkPartner()
                                                        .name()
                                                        .name());
            lenient().when(uploadAttachments.execute(any()))
                     .thenReturn(List.of(MessageAttachmentTestFixtures.createAttachment()));
            lenient().when(businessMessageReceiver.execute(any()))
                     .thenReturn(BusinessMessageTestFixtures.createOutboundMessage());
        }

        @Test
        void should_delete_all_temp_files_after_a_successful_submission() throws IOException {
            backendWebService.submitMessage(
                SoapMessageSubmitTestFixtures.createBackendToConnectorMessage());

            assertThat(remainingTempFiles()).isEmpty();
        }

        @Test
        void should_delete_all_temp_files_when_the_connector_rejects_the_message()
            throws IOException {
            when(businessMessageReceiver.execute(any())).thenThrow(new RuntimeException("Error"));

            backendWebService.submitMessage(
                SoapMessageSubmitTestFixtures.createBackendToConnectorMessage());

            assertThat(remainingTempFiles()).isEmpty();
        }

        @Test
        void should_not_expose_generated_temp_file_names_as_attachment_filenames() {
            backendWebService.submitMessage(
                SoapMessageSubmitTestFixtures.createBackendToConnectorMessage());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<FileUploadCommand>> captor = ArgumentCaptor.forClass(List.class);
            verify(uploadAttachments, atLeastOnce()).execute(captor.capture());

            assertThat(captor.getAllValues().stream().flatMap(List::stream))
                .extracting(command -> (String) ReflectionTestUtils.getField(command, "filename"))
                .isNotEmpty()
                .noneMatch(name -> name.startsWith("ws-payload-")
                    || name.startsWith("ws-attachment-"));
        }
    }
}
