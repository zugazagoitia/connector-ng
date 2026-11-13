/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.message;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.ActionTestFixtures;
import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.PartyTestFixtures;
import eu.ecodex.connector.ServiceTestFixtures;
import eu.ecodex.connector.application.exception.pmode.ConnectorProcessingModeVerificationException;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeActionRepository;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModePartyRepository;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeServiceRepository;
import eu.ecodex.connector.domain.model.ProcessingModeVerificationMode;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)

@DisplayName("ConnectorMessageVerifierService")
public class ConnectorBusinessMessageVerifierServiceTest {
    @Mock
    private ConnectorProcessingModeServiceRepository serviceRepository;
    @Mock
    private ConnectorProcessingModeActionRepository actionRepository;
    @Mock
    private ConnectorProcessingModePartyRepository partyRepository;

    @InjectMocks
    private ConnectorBusinessMessageVerifierService verifierService;

    @Nested
    @DisplayName("in RELAXED verification mode")
    class InRelaxedMode {
        @Test
        void should_pass_when_the_message_is_valid() {
            when(serviceRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ServiceTestFixtures.createService());
            when(actionRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ActionTestFixtures.createAction());

            var message = BusinessMessageTestFixtures.createOutboundMessage();

            assertThatCode(() -> verifierService.verify(
                message,
                ProcessingModeVerificationMode.RELAXED
            ))
                .doesNotThrowAnyException();
        }

        @Test
        void should_fail_when_the_service_is_not_found() {
            when(serviceRepository.findByNameAndBusinessDomain(any(), any())).thenReturn(null);

            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> verifierService.verify(
                    BusinessMessageTestFixtures.createOutboundMessage(),
                    ProcessingModeVerificationMode.RELAXED
                )
            );

            verify(serviceRepository).findByNameAndBusinessDomain(any(), any());
            verify(actionRepository, never()).findByNameAndBusinessDomain(any(), any());
            verify(partyRepository, never())
                .findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any());
        }

        @Test
        void should_fail_when_the_action_is_not_found() {
            when(serviceRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ServiceTestFixtures.createService());
            when(actionRepository.findByNameAndBusinessDomain(any(), any())).thenReturn(null);

            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> verifierService.verify(
                    BusinessMessageTestFixtures.createOutboundMessage(),
                    ProcessingModeVerificationMode.RELAXED
                )
            );

            verify(serviceRepository).findByNameAndBusinessDomain(any(), any());
            verify(actionRepository).findByNameAndBusinessDomain(any(), any());
            verify(partyRepository, never())
                .findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("in STRICT verification mode")
    class InStrictMode {
        @Test
        void should_pass_when_the_message_is_valid() {
            when(serviceRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ServiceTestFixtures.createService());
            when(actionRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ActionTestFixtures.createAction());
            when(partyRepository.findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any()))
                .thenReturn(PartyTestFixtures.createToParty(), PartyTestFixtures.createFromParty());

            var message = BusinessMessageTestFixtures.createOutboundMessage();

            verifierService.verify(message, ProcessingModeVerificationMode.STRICT);

            verify(serviceRepository).findByNameAndBusinessDomain(any(), any());
            verify(actionRepository).findByNameAndBusinessDomain(any(), any());
            verify(partyRepository, times(2))
                .findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any());
        }

        @Test
        void should_fail_when_the_to_party_is_not_found() {
            when(serviceRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ServiceTestFixtures.createService());
            when(actionRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ActionTestFixtures.createAction());
            when(partyRepository.findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any()))
                .thenReturn(null);

            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> verifierService.verify(
                    BusinessMessageTestFixtures.createOutboundMessage(),
                    ProcessingModeVerificationMode.STRICT
                )
            );

            verify(serviceRepository).findByNameAndBusinessDomain(any(), any());
            verify(actionRepository).findByNameAndBusinessDomain(any(), any());
            verify(partyRepository)
                .findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any());
        }

        @Test
        void should_fail_when_the_to_party_identifier_type_is_empty() {
            when(serviceRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ServiceTestFixtures.createService());
            when(actionRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ActionTestFixtures.createAction());
            when(partyRepository.findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any()))
                .thenReturn(null);

            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> verifierService.verify(
                    BusinessMessageTestFixtures.createEmptyToPartyOutboundBusinessMessage(),
                    ProcessingModeVerificationMode.STRICT
                )
            );

            verify(serviceRepository).findByNameAndBusinessDomain(any(), any());
            verify(actionRepository).findByNameAndBusinessDomain(any(), any());
            verify(partyRepository)
                .findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any());
        }

        @Test
        void should_fail_when_the_from_party_is_not_found() {
            when(serviceRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ServiceTestFixtures.createService());
            when(actionRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ActionTestFixtures.createAction());
            when(partyRepository.findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any()))
                .thenReturn(PartyTestFixtures.createToParty(), (ConnectorProcessingModeParty) null);

            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> verifierService.verify(
                    BusinessMessageTestFixtures.createOutboundMessage(),
                    ProcessingModeVerificationMode.STRICT
                )
            );

            verify(partyRepository, times(2))
                .findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any());
        }

        @Test
        void should_fail_when_the_from_party_identifier_type_is_empty() {
            when(serviceRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ServiceTestFixtures.createService());
            when(actionRepository.findByNameAndBusinessDomain(any(), any()))
                .thenReturn(ActionTestFixtures.createAction());
            when(partyRepository.findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any()))
                .thenReturn(PartyTestFixtures.createToParty(), (ConnectorProcessingModeParty) null);

            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> verifierService.verify(
                    BusinessMessageTestFixtures.createEmptyFromPartyOutboundMessage(),
                    ProcessingModeVerificationMode.STRICT
                )
            );

            verify(partyRepository, times(2))
                .findByIdentifierAndRoleTypeAndBusinessDomain(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("in CREATE verification mode")
    class InCreateMode {
        @Test
        void should_fail_because_the_mode_is_not_supported() {
            assertThrows(
                ConnectorProcessingModeVerificationException.class,
                () -> verifierService.verify(
                    BusinessMessageTestFixtures.createOutboundMessage(),
                    ProcessingModeVerificationMode.CREATE
                )
            );
        }
    }

    @Nested
    @DisplayName("when the input is invalid")
    class WhenInputIsInvalid {
        @Test
        void should_fail_when_the_message_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> verifierService.verify(null, ProcessingModeVerificationMode.CREATE)
            );
        }

        @Test
        void should_fail_when_the_verification_mode_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> verifierService.verify(
                    BusinessMessageTestFixtures.createOutboundMessage(),
                    null
                )
            );
        }
    }
}

