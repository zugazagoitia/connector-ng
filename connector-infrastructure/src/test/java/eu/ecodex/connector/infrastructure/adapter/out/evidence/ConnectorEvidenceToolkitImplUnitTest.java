/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.EvidenceTestFixtures;
import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.MessageContentTestFixtures;
import eu.ecodex.connector.application.exception.evidence.ConnectorEvidenceException;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.config.properties.ConnectorEvidencesPropertiesTestFixture;
import eu.ecodex.connector.domain.model.ConnectorMessageRejectionReason;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorEvidenceType;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import eu.ecodex.connector.infrastructure.adapter.out.evidence.builder.ConnectorEvidenceBuilder;
import eu.ecodex.connector.infrastructure.adapter.out.evidence.exception.ConnectorEvidenceBuilderException;
import eu.ecodex.connector.infrastructure.adapter.out.evidence.model.ConnectorEvidenceMessageDetails;
import eu.ecodex.connector.infrastructure.config.property.evidence.ConnectorEvidencesProperties;
import eu.ecodex.connector.infrastructure.helper.HashValueBuilder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.etsi.uri._02640.v2.EventReasonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorEvidenceToolkit")
@MockitoSettings(strictness = Strictness.LENIENT)
class ConnectorEvidenceToolkitImplUnitTest {
    private static final byte[] STUB_EVIDENCE_BYTES = {0x01, 0x02};
    private static final byte[] PREV = {0x0e};
    private static final String HASH_HEX = "abab";
    private static final String PRIOR_EVIDENCE_ATTACHMENT_ID = "prior-attachment-for-test";
    private static final String BUSINESS_XML_ATTACHMENT_ID =
        "104ebc70-abd5-45da-8c74-940d687501b3_messageContent";
    private static final String FIXTURE_SUBMISSION_ACCEPTANCE_ATTACHMENT_ID =
        "c3e18064-e0da-4170-9733-1e7e2768e0bb_SUBMISSION_ACCEPTANCE";

    @Mock
    private ConnectorEvidenceBuilder evidenceBuilder;
    @Mock
    private HashValueBuilder hashValueBuilder;
    @Mock
    private ConnectorMessageAttachmentRepository attachmentRepository;
    @Mock
    private ConnectorFileStorageProvider fileStorageProvider;

    private ConnectorEvidenceToolkitImpl toolkit;

    @BeforeEach
    void setUp() throws ConnectorEvidenceBuilderException {
        when(attachmentRepository.save(any(ConnectorMessageAttachment.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageProvider.findByIdentifier(PRIOR_EVIDENCE_ATTACHMENT_ID)).thenReturn(PREV);
        when(fileStorageProvider.findByIdentifier(FIXTURE_SUBMISSION_ACCEPTANCE_ATTACHMENT_ID))
            .thenReturn(PREV);
        when(fileStorageProvider.findByIdentifier(BUSINESS_XML_ATTACHMENT_ID))
            .thenReturn("<?xml version=\"1.0\"?><root/>".getBytes(StandardCharsets.UTF_8));

        when(hashValueBuilder.getAlgorithm()).thenReturn("SHA-256");
        defaultEvidenceBuilderStubs();

        var evidencesProperties = ConnectorEvidencesPropertiesTestFixture.evidencesProperties();

        toolkit = new ConnectorEvidenceToolkitImpl(
            attachmentRepository,
            fileStorageProvider,
            evidenceBuilder,
            hashValueBuilder,
            evidencesProperties
        );
    }

    private void defaultEvidenceBuilderStubs() throws ConnectorEvidenceBuilderException {
        when(evidenceBuilder.createSubmissionAcceptanceRejection(
            eq(true), nullable(EventReasonType.class), any(), any()
        )).thenReturn(STUB_EVIDENCE_BYTES);
        when(evidenceBuilder.createSubmissionAcceptanceRejection(
            eq(false), any(EventReasonType.class), any(), any()
        )).thenReturn(STUB_EVIDENCE_BYTES);
        when(evidenceBuilder.createRelayREMMDAcceptanceRejection(
            any(boolean.class), nullable(EventReasonType.class), any(), any()
        )).thenReturn(STUB_EVIDENCE_BYTES);
        when(evidenceBuilder.createRelayREMMDFailure(
            any(EventReasonType.class), any(), any()
        )).thenReturn(STUB_EVIDENCE_BYTES);
        when(evidenceBuilder.createDeliveryNonDeliveryToRecipient(
            any(boolean.class), nullable(EventReasonType.class), any(), any()
        )).thenReturn(STUB_EVIDENCE_BYTES);
        when(evidenceBuilder.createRetrievalNonRetrievalByRecipient(
            any(boolean.class), nullable(EventReasonType.class), any(), any()
        )).thenReturn(STUB_EVIDENCE_BYTES);
    }

    private ConnectorBusinessMessage submissionReadyMessage() {
        var base = BusinessMessageTestFixtures.createOutboundMessage();
        var as4 = base.as4Properties().toBuilder()
                      .ebmsMessageIdentifier("urn:test:ebms")
                      .originalSender("sender@domain")
                      .finalRecipient("recipient@domain")
                      .build();
        return base.toBuilder().as4Properties(as4).build();
    }

    private ConnectorMessageRejectionReason rejectionReasonIfNeeded(ConnectorEvidenceType type) {
        return switch (type) {
            case SUBMISSION_REJECTION, RELAY_REMMD_REJECTION, RELAY_REMMD_FAILURE,
                 NON_DELIVERY, NON_RETRIEVAL -> ConnectorMessageRejectionReason.OTHER;
            default -> null;
        };
    }

    private ConnectorBusinessMessage messageWithPrior(ConnectorEvidenceType priorType) {
        var evidence = ConnectorMessageEvidence.builder()
                                               .type(priorType)
                                               .content(new byte[]{0x0e})
                                               .build();
        return submissionReadyMessage().toBuilder()
                                       .evidences(List.of(evidence))
                                       .build();
    }

    @Nested
    @DisplayName("submission acceptance")
    class SubmissionAcceptance {
        @Test
        void should_build_the_acceptance_with_the_payload_hash() throws Exception {
            var message = submissionReadyMessage().toBuilder()
                                                  .businessContent(MessageContentTestFixtures.createContent())
                                                  .build();
            when(hashValueBuilder.buildHashValueAsString(any(byte[].class))).thenReturn(HASH_HEX);

            var evidence =
                toolkit.create(message, ConnectorEvidenceType.SUBMISSION_ACCEPTANCE, null);

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.SUBMISSION_ACCEPTANCE);
            assertThat(evidence.content()).isNotNull();

            var detailsCaptor = ArgumentCaptor.forClass(ConnectorEvidenceMessageDetails.class);
            verify(evidenceBuilder).createSubmissionAcceptanceRejection(
                eq(true), nullable(EventReasonType.class), any(), detailsCaptor.capture()
            );
            var details = detailsCaptor.getValue();
            assertThat(details.getNationalMessageId()).isEqualTo(message.backendMessageIdentifier());
            assertThat(details.getEbmsMessageId()).isEqualTo("urn:test:ebms");
            assertThat(details.getSenderAddress()).isEqualTo("sender@domain");
            assertThat(details.getRecipientAddress()).isEqualTo("recipient@domain");
            assertThat(details.getHashAlgorithm()).isEqualTo("SHA-256");
            assertThat(details.getHashValue()).containsExactly((byte) 0xab, (byte) 0xab);
        }

        @Test
        void should_omit_the_hash_when_there_is_no_xml_payload() throws Exception {
            var message = submissionReadyMessage();

            var evidence =
                toolkit.create(message, ConnectorEvidenceType.SUBMISSION_ACCEPTANCE, null);

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.SUBMISSION_ACCEPTANCE);
            assertThat(evidence.content()).isNotNull();

            var detailsCaptor = ArgumentCaptor.forClass(ConnectorEvidenceMessageDetails.class);
            verify(evidenceBuilder).createSubmissionAcceptanceRejection(
                eq(true), nullable(EventReasonType.class), any(), detailsCaptor.capture()
            );
            assertThat(detailsCaptor.getValue().getHashValue()).isNull();
        }
    }

    @Nested
    @DisplayName("submission rejection")
    class SubmissionRejection {
        @Test
        void should_map_the_reason_to_an_event() throws Exception {
            var message = submissionReadyMessage();
            var detailsCaptor = ArgumentCaptor.forClass(ConnectorEvidenceMessageDetails.class);
            var reasonCaptor = ArgumentCaptor.forClass(EventReasonType.class);

            var evidence = toolkit.create(
                message, ConnectorEvidenceType.SUBMISSION_REJECTION,
                ConnectorMessageRejectionReason.BACKEND_REJECTION
            );

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.SUBMISSION_REJECTION);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createSubmissionAcceptanceRejection(
                eq(false), reasonCaptor.capture(), any(), detailsCaptor.capture()
            );
            assertThat(reasonCaptor.getValue().getCode())
                .isEqualTo(ConnectorMessageRejectionReason.BACKEND_REJECTION.getErrorCode());
            assertThat(reasonCaptor.getValue().getDetails())
                .isEqualTo(ConnectorMessageRejectionReason.BACKEND_REJECTION.getReason());
        }

        @Test
        void should_use_the_enum_name_when_the_reason_text_is_blank() throws Exception {
            var message = submissionReadyMessage();
            var reasonCaptor = ArgumentCaptor.forClass(EventReasonType.class);

            var evidence = toolkit.create(
                message, ConnectorEvidenceType.SUBMISSION_REJECTION,
                ConnectorMessageRejectionReason.UNSPECIFIC_PROCESSING_ERROR
            );

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.SUBMISSION_REJECTION);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createSubmissionAcceptanceRejection(
                eq(false), reasonCaptor.capture(), any(), any()
            );
            assertThat(reasonCaptor.getValue().getDetails())
                .isEqualTo(ConnectorMessageRejectionReason.UNSPECIFIC_PROCESSING_ERROR.name());
        }
    }

    @Nested
    @DisplayName("relay REMMD")
    class RelayRemmd {
        @Test
        void should_build_the_acceptance_from_the_submission_acceptance() throws Exception {
            var message = messageWithPrior(ConnectorEvidenceType.SUBMISSION_ACCEPTANCE);

            var evidence =
                toolkit.create(message, ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE, null);

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createRelayREMMDAcceptanceRejection(
                eq(true), nullable(EventReasonType.class), any(), eq(PREV)
            );
        }

        @Test
        void should_map_the_reason_for_the_rejection() throws Exception {
            var message = messageWithPrior(ConnectorEvidenceType.SUBMISSION_ACCEPTANCE);
            var reasonCaptor = ArgumentCaptor.forClass(EventReasonType.class);

            var evidence = toolkit.create(
                message, ConnectorEvidenceType.RELAY_REMMD_REJECTION,
                ConnectorMessageRejectionReason.GW_REJECTION
            );

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.RELAY_REMMD_REJECTION);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createRelayREMMDAcceptanceRejection(
                eq(false), reasonCaptor.capture(), any(), eq(PREV)
            );
            assertThat(reasonCaptor.getValue().getCode()).isEqualTo("E201");
        }

        @Test
        void should_call_the_failure_builder() throws Exception {
            var message = messageWithPrior(ConnectorEvidenceType.SUBMISSION_ACCEPTANCE);
            var reasonCaptor = ArgumentCaptor.forClass(EventReasonType.class);

            var evidence = toolkit.create(
                message, ConnectorEvidenceType.RELAY_REMMD_FAILURE,
                ConnectorMessageRejectionReason.RELAY_REMMD_TIMEOUT
            );

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.RELAY_REMMD_FAILURE);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createRelayREMMDFailure(
                reasonCaptor.capture(),
                any(),
                eq(PREV)
            );
            assertThat(reasonCaptor.getValue().getCode()).isEqualTo("E301");
        }
    }

    @Nested
    @DisplayName("delivery and retrieval")
    class DeliveryAndRetrieval {
        @Test
        void should_build_the_delivery_from_the_relay_acceptance() throws Exception {
            var message = messageWithPrior(ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE);

            var evidence = toolkit.create(message, ConnectorEvidenceType.DELIVERY, null);

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.DELIVERY);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createDeliveryNonDeliveryToRecipient(
                eq(true), nullable(EventReasonType.class), any(), eq(PREV)
            );
        }

        @Test
        void should_build_the_retrieval_from_the_delivery() throws Exception {
            var message = messageWithPrior(ConnectorEvidenceType.DELIVERY);

            var evidence = toolkit.create(message, ConnectorEvidenceType.RETRIEVAL, null);

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.RETRIEVAL);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createRetrievalNonRetrievalByRecipient(
                eq(true), nullable(EventReasonType.class), any(), eq(PREV)
            );
        }

        @Test
        void should_map_the_reason_for_a_non_delivery() throws Exception {
            var message = messageWithPrior(ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE);
            var reasonCaptor = ArgumentCaptor.forClass(EventReasonType.class);

            var evidence = toolkit.create(
                message, ConnectorEvidenceType.NON_DELIVERY,
                ConnectorMessageRejectionReason.UNREACHABLE
            );

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.NON_DELIVERY);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createDeliveryNonDeliveryToRecipient(
                eq(false), reasonCaptor.capture(), any(), eq(PREV)
            );
            assertThat(reasonCaptor.getValue().getCode()).isEqualTo("E404");
        }

        @Test
        void should_map_the_reason_for_a_non_retrieval() throws Exception {
            var message = messageWithPrior(ConnectorEvidenceType.DELIVERY);
            var reasonCaptor = ArgumentCaptor.forClass(EventReasonType.class);

            var evidence = toolkit.create(
                message, ConnectorEvidenceType.NON_RETRIEVAL,
                ConnectorMessageRejectionReason.DELIVERY_EVIDENCE_TIMEOUT
            );

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.NON_RETRIEVAL);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createRetrievalNonRetrievalByRecipient(
                eq(false), reasonCaptor.capture(), any(), eq(PREV)
            );
            assertThat(reasonCaptor.getValue().getCode()).isEqualTo("E300");
        }
    }

    @Nested
    @DisplayName("input validation")
    class InputValidation {
        static Stream<Arguments> rejectionEvidenceTypes() {
            return Stream.of(
                Arguments.of(ConnectorEvidenceType.SUBMISSION_REJECTION),
                Arguments.of(ConnectorEvidenceType.RELAY_REMMD_REJECTION),
                Arguments.of(ConnectorEvidenceType.RELAY_REMMD_FAILURE),
                Arguments.of(ConnectorEvidenceType.NON_DELIVERY),
                Arguments.of(ConnectorEvidenceType.NON_RETRIEVAL)
            );
        }

        @ParameterizedTest
        @MethodSource("rejectionEvidenceTypes")
        void should_require_a_reason_for_rejection_evidence(ConnectorEvidenceType type) {
            var message = messageWithPrior(ConnectorEvidenceType.SUBMISSION_ACCEPTANCE);

            assertThatThrownBy(() -> toolkit.create(message, type, null))
                .isInstanceOf(ConnectorEvidenceException.class)
                .hasMessageContaining("RejectionReason may not be null");
        }

        @Test
        void should_require_a_non_blank_national_message_id() {
            var message =
                submissionReadyMessage().toBuilder().backendMessageIdentifier("  ").build();

            assertThatThrownBy(
                () -> toolkit.create(message, ConnectorEvidenceType.SUBMISSION_ACCEPTANCE, null))
                .isInstanceOf(ConnectorEvidenceException.class)
                .hasMessageContaining("nationalMessageId");
        }

        @Test
        void should_require_a_non_blank_final_recipient() {
            var as4 =
                submissionReadyMessage().as4Properties().toBuilder().finalRecipient("").build();
            var message = submissionReadyMessage().toBuilder().as4Properties(as4).build();

            assertThatThrownBy(
                () -> toolkit.create(message, ConnectorEvidenceType.SUBMISSION_ACCEPTANCE, null))
                .isInstanceOf(ConnectorEvidenceException.class)
                .hasMessageContaining("finalRecipient");
        }

        @Test
        void should_require_a_non_blank_original_sender() {
            var as4 =
                submissionReadyMessage().as4Properties().toBuilder().originalSender(null).build();
            var message = submissionReadyMessage().toBuilder().as4Properties(as4).build();

            assertThatThrownBy(
                () -> toolkit.create(message, ConnectorEvidenceType.SUBMISSION_ACCEPTANCE, null))
                .isInstanceOf(ConnectorEvidenceException.class)
                .hasMessageContaining("originalSender");
        }
    }

    @Nested
    @DisplayName("predecessor resolution")
    class PredecessorResolution {
        static Stream<Arguments> missingPredecessorCases() {
            return Stream.of(
                Arguments.of(
                    ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE,
                    ConnectorEvidenceType.DELIVERY
                ),
                Arguments.of(
                    ConnectorEvidenceType.DELIVERY,
                    ConnectorEvidenceType.SUBMISSION_ACCEPTANCE
                ),
                Arguments.of(
                    ConnectorEvidenceType.RETRIEVAL,
                    ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE
                ),
                Arguments.of(
                    ConnectorEvidenceType.RELAY_REMMD_FAILURE,
                    ConnectorEvidenceType.DELIVERY
                )
            );
        }

        @ParameterizedTest
        @MethodSource("missingPredecessorCases")
        void should_fail_when_the_predecessor_is_missing(
            ConnectorEvidenceType toCreate,
            ConnectorEvidenceType present) {
            var message = messageWithPrior(present);

            assertThatThrownBy(() -> toolkit.create(
                message,
                toCreate,
                rejectionReasonIfNeeded(toCreate)
            ))
                .isInstanceOf(ConnectorEvidenceException.class)
                .hasMessageContaining("no prior evidence of type");
        }

        @Test
        void should_fail_when_the_evidences_list_is_null() {
            var message = submissionReadyMessage().toBuilder().evidences(null).build();

            assertThatThrownBy(
                () -> toolkit.create(message, ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE, null))
                .isInstanceOf(ConnectorEvidenceException.class)
                .hasMessageContaining("no prior evidence");
        }

        @Test
        void should_skip_entries_with_null_content_when_resolving_the_predecessor()
            throws Exception {
            var emptyEntry = ConnectorMessageEvidence.builder()
                                                     .type(ConnectorEvidenceType.SUBMISSION_ACCEPTANCE)
                                                     .content(new byte[]{0x0e})
                                                     .build();
            var good = EvidenceTestFixtures.createSubmissionAcceptanceEvidence();
            var evidences = new ArrayList<ConnectorMessageEvidence>();
            evidences.add(emptyEntry);
            evidences.add(good);
            var message = submissionReadyMessage().toBuilder().evidences(evidences).build();

            var evidence =
                toolkit.create(message, ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE, null);

            assertThat(evidence).isNotNull();
            assertThat(evidence.type()).isEqualTo(ConnectorEvidenceType.RELAY_REMMD_ACCEPTANCE);
            assertThat(evidence.content()).isNotNull();

            verify(evidenceBuilder).createRelayREMMDAcceptanceRejection(
                eq(true), nullable(EventReasonType.class), any(), eq(PREV)
            );
        }
    }

    @Nested
    @DisplayName("error wrapping")
    class ErrorWrapping {
        @Test
        void should_wrap_a_hash_builder_failure() {
            var xmlContent = "<?xml version=\"1.0\"?><root/>";
            var xmlAttachment = MessageAttachmentTestFixtures.createBusinessContentAttachment()
                                                             .toBuilder()
                                                             .identifier("hash-fail-xml-id")
                                                             .build();
            var businessContent = ConnectorMessageBusinessContent.builder()
                                                                 .xmlContent(xmlAttachment)
                                                                 .build();
            var messageWithXml = submissionReadyMessage().toBuilder()
                                                         .businessContent(businessContent)
                                                         .build();

            when(fileStorageProvider.findByIdentifier("hash-fail-xml-id"))
                .thenReturn(xmlContent.getBytes(StandardCharsets.UTF_8));
            when(hashValueBuilder.buildHashValueAsString(
                xmlContent.getBytes(StandardCharsets.UTF_8)))
                .thenThrow(new IllegalStateException("hash boom"));

            assertThatThrownBy(() -> toolkit.create(
                messageWithXml,
                ConnectorEvidenceType.SUBMISSION_ACCEPTANCE,
                null
            ))
                .isInstanceOf(ConnectorEvidenceException.class)
                .hasMessageContaining("could not build payload hash");
        }

        @Test
        void should_wrap_an_evidence_builder_exception() throws Exception {
            var message = submissionReadyMessage();
            when(evidenceBuilder.createSubmissionAcceptanceRejection(
                eq(true), nullable(EventReasonType.class), any(), any()
            )).thenThrow(new ConnectorEvidenceBuilderException("x"));

            assertThatThrownBy(() -> toolkit.create(
                message,
                ConnectorEvidenceType.SUBMISSION_ACCEPTANCE,
                null
            ))
                .isInstanceOf(ConnectorEvidenceException.class)
                .hasMessageContaining("evidence could not be created")
                .hasCauseInstanceOf(ConnectorEvidenceBuilderException.class);
        }
    }
}
