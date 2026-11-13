/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector;

import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod", "LineLength"})
public class BusinessMessageTestFixtures {
    public static ConnectorBusinessMessage createOutboundMessage() {
        var builder = backendToGatewayMessage();
        return builder.build();
    }

    public static ConnectorBusinessMessage createEvidenceMessage() {
        var builder = backendToGatewayMessage();
        return builder
            .businessContent(MessageContentTestFixtures.createContent())
            .evidences(null)
            .transportedEvidences(
                List.of(EvidenceTestFixtures.createDeliveryEvidence())
            )
            .build();
    }

    public static ConnectorBusinessMessage createInboundMessage() {
        return backendToGatewayMessage()
            .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
            .attachments(List.of(
                MessageAttachmentTestFixtures.createXMLTrustOKTokenAttachment()
            ))
            .transportedEvidences(
                List.of(EvidenceTestFixtures.createRelayREMMDAcceptanceEvidence())
            ).build();
    }

    public static ConnectorBusinessMessage createValidatedInboundMessage() {
        return createInboundMessage()
            .toBuilder()
            .attachments(List.of(
                MessageAttachmentTestFixtures.createPDFTrustOKTokenAttachment(),
                MessageAttachmentTestFixtures.createXMLTrustOKTokenAttachment()
            ))
            .transportedEvidences(
                List.of(EvidenceTestFixtures.createRelayREMMDAcceptanceEvidence())
            ).build();
    }

    public static ConnectorBusinessMessage businessMessageWithoutBackendName() {
        return createInboundMessage().toBuilder()
                                     .backendName(null)
                                     .build();
    }

    public static ConnectorBusinessMessage createInboundMessageWithoutBackendNameAndConversationIdentifier() {
        return businessMessageWithoutBackendName()
            .toBuilder()
            .backendName(null)
            .as4Properties(AS4PropertiesTestFixtures.defaultAS4Properties()
                                                    .conversationIdentifier(null)
                                                    .build())
            .build();
    }

    public static ConnectorBusinessMessage createValidOutboundMessageWithoutGatewayName() {
        return backendToGatewayMessage().gatewayName(null).build();
    }

    public static ConnectorBusinessMessage createEmptyFromPartyOutboundMessage() {
        return backendToGatewayMessage()
            .as4Properties(
                AS4PropertiesTestFixtures.defaultAS4Properties()
                                         .fromParty(
                                             PartyTestFixtures.createFromParty()
                                                              .toBuilder()
                                                              .identifierType("")
                                                              .build())
                                         .build()
            )
            .build();
    }

    public static ConnectorBusinessMessage createEmptyToPartyOutboundBusinessMessage() {
        return backendToGatewayMessage()
            .as4Properties(
                AS4PropertiesTestFixtures.defaultAS4Properties()
                                         .toParty(PartyTestFixtures.createToParty()
                                                                   .toBuilder()
                                                                   .identifierType("")
                                                                   .build())
                                         .build()
            )
            .build();
    }

    public static ConnectorBusinessMessage createInvalidFromPartyOutboundBusinessMessage() {
        return backendToGatewayMessage()
            .as4Properties(
                AS4PropertiesTestFixtures.defaultAS4Properties()
                                         .fromParty(PartyTestFixtures.createToParty())
                                         .build()
            )
            .build();
    }

    public static ConnectorBusinessMessage createInvalidToPartyOutboundBusinessMessage() {
        return backendToGatewayMessage()
            .as4Properties(
                AS4PropertiesTestFixtures.defaultAS4Properties()
                                         .toParty(PartyTestFixtures.createFromParty())
                                         .build()
            ).build();
    }

    public static ConnectorBusinessMessage createRejectedMessage() {
        var evidence = EvidenceTestFixtures.createSubmissionRejectionEvidence();
        var evidences = new ArrayList<ConnectorMessageEvidence>();
        evidences.add(evidence);

        return backendToGatewayMessage()
            .evidences(evidences)
            .transportedEvidences(evidences)
            .rejectedAt(Instant.now())
            .build();
    }

    public static ConnectorBusinessMessage createConfirmedMessage() {
        var evidence = EvidenceTestFixtures.createSubmissionAcceptanceEvidence();
        var evidences = new ArrayList<ConnectorMessageEvidence>();
        evidences.add(evidence);

        return backendToGatewayMessage()
            .evidences(evidences)
            .transportedEvidences(evidences)
            .confirmedAt(Instant.now())
            .build();
    }

    public static ConnectorBusinessMessage createMessageWithSubmissionAcceptanceEvidence() {
        var evidence = EvidenceTestFixtures.createSubmissionAcceptanceEvidence();
        var evidences = new ArrayList<ConnectorMessageEvidence>();
        evidences.add(evidence);

        return backendToGatewayMessage()
            .transportedEvidences(evidences)
            .build();
    }

    // is considered as an outgoing message
    private static ConnectorBusinessMessage.ConnectorBusinessMessageBuilder backendToGatewayMessage() {
        return ConnectorBusinessMessage
            .builder()
            .businessDomainIdentifier(
                BusinessDomainTestFixtures.createDefaultBusinessDomain()
                                          .identifier()
            )
            .businessContent(MessageContentTestFixtures.createContent())
            .identifier("223caef9-cae9-4387-a38c-ad4879f94b4e@connector.ecodex.eu")
            .backendMessageIdentifier(
                "85964ab5-b04b-4d45-97d1-962b565e22df@connector.ecodex.eu")
            .backendName("default_backend")
            .gatewayName("default_gateway")
            .as4Properties(AS4PropertiesTestFixtures.defaultAS4Properties().build())
            .direction(ConnectorMessageDirection.BACKEND_TO_GATEWAY)
            .status(ConnectorBusinessMessageStatus.CREATED)
            .evidences(new ArrayList<>())
            .transportedEvidences(new ArrayList<>())
            .createdAt(Instant.now())
            .updatedAt(Instant.now());
    }
}
