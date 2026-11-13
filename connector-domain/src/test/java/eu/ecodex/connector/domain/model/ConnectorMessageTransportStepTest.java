/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.domain.model;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ConnectorMessageTransportStep")
class ConnectorMessageTransportStepTest {
    private static final String STEP_ID = "step-001";
    private static final String LINK_PARTNER = "default_test_backend";
    private static final String MESSAGE_ID = "msg-001";
    private static final String BACKEND_ID = "backend-ref-001";
    private static final String EBMS_ID = "ebms-001@domibus.eu";
    private static final String PROVIDED_REMOTE_ID = "remote-provided-001";

    private ConnectorBusinessMessage messageSentToGateway() {
        var message = BusinessMessageTestFixtures.createInboundMessage();
        return message.toBuilder()
                      .identifier(MESSAGE_ID)
                      .direction(ConnectorMessageDirection.BACKEND_TO_GATEWAY)
                      .as4Properties(
                          message.as4Properties().toBuilder()
                                 .ebmsMessageIdentifier(EBMS_ID)
                                 .build()
                      )
                      .build();
    }

    private ConnectorBusinessMessage messageSentToBackend() {
        return BusinessMessageTestFixtures.createInboundMessage()
                                          .toBuilder()
                                          .identifier(MESSAGE_ID)
                                          .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                          .backendMessageIdentifier(BACKEND_ID)
                                          .build();
    }

    private ConnectorMessageTransportStep.ConnectorMessageTransportStepBuilder stepBuilder(
        ConnectorMessage transportedMessage) {
        return ConnectorMessageTransportStep.builder()
                                            .identifier(STEP_ID)
                                            .transportedMessageIdentifier(MESSAGE_ID)
                                            .remoteSystemIdentifier(PROVIDED_REMOTE_ID)
                                            .linkPartnerName(LINK_PARTNER)
                                            .transportedMessage(transportedMessage)
                                            .status(ConnectorMessageTransportStatus.SUBMITTED)
                                            .numberOfAttempts(1);
    }

    @Nested
    @DisplayName("remote system identifier")
    class RemoteSystemIdentifier {
        @Test
        void should_keep_the_given_remote_system_identifier_when_it_is_not_blank() {
            var step = stepBuilder(messageSentToGateway()).build();

            assertThat(step.remoteSystemIdentifier()).isEqualTo(PROVIDED_REMOTE_ID);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void should_use_the_ebms_message_identifier_when_the_remote_identifier_is_blank_and_the_message_is_sent_to_the_gateway(
            String blankIdentifier) {
            var step = stepBuilder(messageSentToGateway())
                .remoteSystemIdentifier(blankIdentifier)
                .build();

            assertThat(step.remoteSystemIdentifier()).isEqualTo(EBMS_ID);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void should_use_the_backend_message_identifier_when_the_remote_identifier_is_blank_and_the_message_is_sent_to_the_backend(
            String blankIdentifier) {
            var step = stepBuilder(messageSentToBackend())
                .remoteSystemIdentifier(blankIdentifier)
                .build();

            assertThat(step.remoteSystemIdentifier()).isEqualTo(BACKEND_ID);
        }

        @Test
        void should_fail_when_the_remote_identifier_is_blank_and_the_transported_message_is_null() {
            assertThatThrownBy(() -> stepBuilder(null).remoteSystemIdentifier(null)).isInstanceOf(
                NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("transported message identifier")
    class TransportedMessageIdentifier {
        @Test
        void should_keep_the_given_transported_message_identifier_when_it_is_not_blank() {
            var step = stepBuilder(messageSentToGateway())
                .transportedMessageIdentifier("other-msg-002")
                .build();

            assertThat(step.transportedMessageIdentifier()).isEqualTo("other-msg-002");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void should_use_the_identifier_of_the_transported_message_when_the_given_one_is_blank(
            String blankIdentifier) {
            var step = stepBuilder(messageSentToGateway())
                .transportedMessageIdentifier(blankIdentifier)
                .build();

            assertThat(step.transportedMessageIdentifier()).isEqualTo(MESSAGE_ID);
        }

        @Test
        void should_fail_when_the_transported_message_is_null() {
            assertThatThrownBy(() -> {
                var builder = stepBuilder(null);
                builder.build();
            }).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("when both identifiers are omitted")
    class WhenBothIdentifiersAreOmitted {
        @Test
        void should_resolve_both_identifiers_from_the_message_sent_to_the_gateway() {
            var step = stepBuilder(messageSentToGateway())
                .remoteSystemIdentifier(null)
                .transportedMessageIdentifier(null)
                .build();

            assertThat(step.remoteSystemIdentifier()).isEqualTo(EBMS_ID);
            assertThat(step.transportedMessageIdentifier()).isEqualTo(MESSAGE_ID);
        }

        @Test
        void should_resolve_both_identifiers_from_the_message_sent_to_the_backend() {
            var step = stepBuilder(messageSentToBackend())
                .remoteSystemIdentifier(null)
                .transportedMessageIdentifier(null)
                .build();

            assertThat(step.remoteSystemIdentifier()).isEqualTo(BACKEND_ID);
            assertThat(step.transportedMessageIdentifier()).isEqualTo(MESSAGE_ID);
        }
    }

    @Nested
    @DisplayName("toBuilder")
    class ToBuilder {
        @Test
        void should_keep_the_resolved_identifiers_when_the_step_is_copied() {
            var original = stepBuilder(messageSentToBackend())
                .remoteSystemIdentifier(null)
                .transportedMessageIdentifier(null)
                .build();

            var copy = original.toBuilder().numberOfAttempts(2).build();

            assertThat(copy.remoteSystemIdentifier()).isEqualTo(BACKEND_ID);
            assertThat(copy.transportedMessageIdentifier()).isEqualTo(MESSAGE_ID);
            assertThat(copy.numberOfAttempts()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringRepresentation {
        @Test
        void should_contain_the_step_data_and_not_the_transported_message() {
            var createdAt = Instant.parse("2026-11-10T12:00:00Z");
            var step = stepBuilder(messageSentToGateway())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .statuses(Set.of())
                .build();

            assertThat(step.toString())
                .contains("identifier=" + STEP_ID)
                .contains("numberOfAttempts=1")
                .contains("createdAt=" + createdAt)
                .doesNotContain(LINK_PARTNER)
                .doesNotContain(PROVIDED_REMOTE_ID);
        }
    }
}
