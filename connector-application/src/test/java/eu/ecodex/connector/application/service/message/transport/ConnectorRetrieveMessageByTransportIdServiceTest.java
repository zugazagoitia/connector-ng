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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.application.exception.NotFoundException;
import eu.ecodex.connector.application.exception.message.ConnectorLinkPartnerNotOwnerException;
import eu.ecodex.connector.application.exception.message.ConnectorMessageTransportStepException;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
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
@DisplayName("ConnectorRetrieveMessageByTransportIdService")
class ConnectorRetrieveMessageByTransportIdServiceTest {
    private static final String TRANSPORT_ID = "step-001";
    private static final String MESSAGE_ID = "msg-001";
    private static final String LINK_PARTNER = "default_test_backend";

    @Mock
    private ConnectorMessageTransportStepRepository transportStepRepository;

    @InjectMocks
    private ConnectorRetrieveMessageByTransportIdService service;

    private ConnectorBusinessMessage businessMessage() {
        return BusinessMessageTestFixtures.createInboundMessage()
                                          .toBuilder()
                                          .identifier(MESSAGE_ID)
                                          .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                          .build();
    }

    private ConnectorMessageTransportStep step(
        ConnectorMessageTransportStatus status,
        String linkPartnerName,
        ConnectorMessage transportedMessage) {
        return ConnectorMessageTransportStep.builder()
                                            .identifier(TRANSPORT_ID)
                                            .transportedMessageIdentifier(MESSAGE_ID)
                                            .remoteSystemIdentifier("remote-001")
                                            .linkPartnerName(linkPartnerName)
                                            .transportedMessage(transportedMessage)
                                            .status(status)
                                            .numberOfAttempts(0)
                                            .build();
    }

    @Nested
    @DisplayName("when the input is invalid")
    class WhenInputIsInvalid {
        @Test
        void should_fail_when_the_transport_identifier_is_null() {
            assertThatThrownBy(() -> service.execute(null, LINK_PARTNER))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transportIdentifier");

            verifyNoInteractions(transportStepRepository);
        }
    }

    @Nested
    @DisplayName("when the transport step is not found")
    class WhenStepNotFound {
        @Test
        void should_fail_when_no_transport_step_exists_for_the_identifier() {
            when(transportStepRepository.findByIdentifier(TRANSPORT_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.execute(TRANSPORT_ID, LINK_PARTNER))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(TRANSPORT_ID);
        }
    }

    @Nested
    @DisplayName("when the link partner does not own the transport step")
    class WhenLinkPartnerNotOwner {
        @Test
        void should_fail_when_the_transport_step_belongs_to_another_link_partner() {
            when(transportStepRepository.findByIdentifier(TRANSPORT_ID)).thenReturn(
                step(
                    ConnectorMessageTransportStatus.READY_FOR_PULL,
                    "alice_backend",
                    businessMessage()
                )
            );

            assertThatThrownBy(() -> service.execute(TRANSPORT_ID, LINK_PARTNER))
                .isInstanceOf(ConnectorLinkPartnerNotOwnerException.class)
                .hasMessageContaining(TRANSPORT_ID)
                .hasMessageContaining(LINK_PARTNER);
        }
    }

    @Nested
    @DisplayName("when the transport step is not ready for pull")
    class WhenStepNotReadyForPull {
        @ParameterizedTest
        @EnumSource(
            value = ConnectorMessageTransportStatus.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "READY_FOR_PULL"
        )
        void should_fail_when_the_transport_step_status_is_not_ready_for_pull(
            ConnectorMessageTransportStatus status) {
            when(transportStepRepository.findByIdentifier(TRANSPORT_ID)).thenReturn(
                step(status, LINK_PARTNER, businessMessage())
            );

            assertThatThrownBy(() -> service.execute(TRANSPORT_ID, LINK_PARTNER))
                .isInstanceOf(ConnectorMessageTransportStepException.class)
                .hasMessageContaining(TRANSPORT_ID)
                .hasMessageContaining("READY_FOR_PULL");
        }
    }

    @Nested
    @DisplayName("when the transport step is ready for pull")
    class WhenStepReadyForPull {
        @Test
        void should_return_the_transported_message() {
            var message = businessMessage();
            when(transportStepRepository.findByIdentifier(TRANSPORT_ID)).thenReturn(
                step(ConnectorMessageTransportStatus.READY_FOR_PULL, LINK_PARTNER, message)
            );

            var result = service.execute(TRANSPORT_ID, LINK_PARTNER);

            assertThat(result).isSameAs(message);
            verify(transportStepRepository).findByIdentifier(TRANSPORT_ID);
            verifyNoMoreInteractions(transportStepRepository);
        }

        @Test
        void should_ignore_the_case_of_the_link_partner_name_when_checking_the_ownership() {
            var message = businessMessage();
            when(transportStepRepository.findByIdentifier(TRANSPORT_ID)).thenReturn(
                step(ConnectorMessageTransportStatus.READY_FOR_PULL, LINK_PARTNER, message)
            );

            var result = service.execute(TRANSPORT_ID, LINK_PARTNER.toUpperCase());

            assertThat(result).isSameAs(message);
        }
    }
}
