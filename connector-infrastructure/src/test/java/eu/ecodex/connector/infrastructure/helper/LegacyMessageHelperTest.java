/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.helper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessDomainTestFixtures;
import eu.ecodex.connector.EvidenceTestFixtures;
import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorEvidenceMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageAS4Properties;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorEvidenceType;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import eu.ecodex.connector.domain.model.pmode.ConnectorPartyRoleType;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeAction;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegacyMessageHelper")
public class LegacyMessageHelperTest {
    private static final String MESSAGE_ID = "msg-001";
    private static final String BACKEND_NAME = "backend-alice";

    @Mock
    private ConnectorMessageAttachmentRepository attachmentRepository;
    @Mock
    private ConnectorFileStorageProvider fileStorageProvider;

    @InjectMocks
    private LegacyMessageHelper legacyMessageHelper;

    private void stubHappyPath() {
        when(fileStorageProvider.findByIdentifier("xml-content-id"))
            .thenReturn("<xml/>".getBytes());
        when(attachmentRepository.findByMessageIdentifierAndTypes(eq(MESSAGE_ID), any()))
            .thenReturn(List.of());
    }

    private ConnectorMessageAS4Properties as4Properties() {
        return ConnectorMessageAS4Properties
            .builder()
            .service(ConnectorProcessingModeService.builder()
                                                   .name("EPO")
                                                   .type("urn:e-codex:services:")
                                                   .build())
            .action(ConnectorProcessingModeAction.builder()
                                                 .name("Form_A")
                                                 .build())
            .fromParty(ConnectorProcessingModeParty.builder()
                                                   .identifier("BL")
                                                   .identifierType(
                                                       "urn:oasis:names:tc:ebcore:partyid-type"
                                                           + ":ecodex")
                                                   .role("GW")
                                                   .roleType(ConnectorPartyRoleType.INITIATOR)
                                                   .build())
            .toParty(ConnectorProcessingModeParty.builder()
                                                 .identifier("RE")
                                                 .identifierType(
                                                     "urn:oasis:names:tc:ebcore:partyid-type:ecodex")
                                                 .role("GW")
                                                 .roleType(ConnectorPartyRoleType.RESPONDER)
                                                 .build())
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
            .xmlContent(ConnectorMessageAttachment.builder()
                                                  .identifier("xml-content-id")
                                                  .build())
            .build();
    }

    private ConnectorBusinessMessage inboundMessage() {
        return ConnectorBusinessMessage.builder()
                                       .businessDomainIdentifier(
                                           BusinessDomainTestFixtures.createDefaultBusinessDomain()
                                                                     .identifier())
                                       .identifier(MESSAGE_ID)
                                       .backendName(BACKEND_NAME)
                                       .backendMessageIdentifier(null)
                                       .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                       .status(ConnectorBusinessMessageStatus.CREATED)
                                       .as4Properties(as4Properties())
                                       .businessContent(businessContent())
                                       .attachments(List.of())
                                       .evidences(List.of())
                                       .build();
    }

    @Nested
    @DisplayName("when converting a business message")
    class WhenConvertingABusinessMessage {
        @Test
        void should_fetch_the_attachments() {
            stubHappyPath();

            legacyMessageHelper.convertMessage(inboundMessage());

            verify(attachmentRepository).findByMessageIdentifierAndTypes(
                eq(MESSAGE_ID),
                argThat(types -> types.containsAll(List.of(
                    ConnectorAttachmentType.ATTACHMENT,
                    ConnectorAttachmentType.PDF_TOKEN,
                    ConnectorAttachmentType.XML_TOKEN
                )))
            );
        }

        @Test
        void should_fail_when_an_evidence_has_no_attachment() {
            var evidence = ConnectorMessageEvidence.builder()
                                                   .type(ConnectorEvidenceType.values()[0])
                                                   .content(null)
                                                   .build();
            var inbound = inboundMessage().toBuilder()
                                          .transportedEvidences(List.of(evidence))
                                          .build();

            stubHappyPath();

            assertThatThrownBy(() -> legacyMessageHelper.convertMessage(inbound))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("when converting an evidence message")
    class WhenConvertingAnEvidenceMessage {
        @Test
        void should_convert_the_confirmations() {
            var inbound = ConnectorEvidenceMessage.builder()
                                                  .identifier(MESSAGE_ID)
                                                  .backendName(BACKEND_NAME)
                                                  .backendMessageIdentifier(null)
                                                  .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                                  .as4Properties(as4Properties())
                                                  .transportedEvidences(List.of(
                                                      EvidenceTestFixtures.createRelayREMMDAcceptanceEvidence()))
                                                  .build();

            var message = legacyMessageHelper.convertMessage(inbound);

            assertThat(message).isNotNull();
            assertThat(message.getMessageConfirmations().size()).isEqualTo(1);
            assertThat(message.getMessageConfirmations().getFirst().getConfirmationType())
                .isNotNull();

            verify(fileStorageProvider, never()).findByIdentifier(any());
        }
    }
}
