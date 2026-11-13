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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorListPendingTransportStepsService")
class ConnectorListPendingTransportStepsServiceTest {
    private static final String BACKEND_NAME = "default_test_backend";
    private static final String MESSAGE_ID = "msg-001";

    @Mock
    private ConnectorMessageTransportStepRepository transportStepRepository;

    @InjectMocks
    private ConnectorListPendingTransportStepsService service;

    private ConnectorMessageTransportStep step(String identifier) {
        var message = BusinessMessageTestFixtures.createInboundMessage()
                                                 .toBuilder()
                                                 .identifier(MESSAGE_ID)
                                                 .direction(ConnectorMessageDirection.GATEWAY_TO_BACKEND)
                                                 .build();
        return ConnectorMessageTransportStep.builder()
                                            .identifier(identifier)
                                            .transportedMessageIdentifier(MESSAGE_ID)
                                            .remoteSystemIdentifier("remote-001")
                                            .linkPartnerName(BACKEND_NAME)
                                            .transportedMessage(message)
                                            .status(ConnectorMessageTransportStatus.READY_FOR_PULL)
                                            .numberOfAttempts(0)
                                            .build();
    }

    @Nested
    @DisplayName("when the backend name is invalid")
    class WhenBackendNameIsInvalid {
        @Test
        void should_fail_when_the_backend_name_is_null() {
            assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("backendName");

            verifyNoInteractions(transportStepRepository);
        }
    }

    @Nested
    @DisplayName("when the backend name is valid")
    class WhenBackendNameIsValid {
        @Test
        void should_return_the_steps_ready_to_download_for_the_backend() {
            var steps = List.of(step("step-001"), step("step-002"));
            when(transportStepRepository.findReadyToDownloadTransportSteps(BACKEND_NAME))
                .thenReturn(steps);

            var result = service.execute(BACKEND_NAME);

            assertThat(result).containsExactlyElementsOf(steps);
            verify(transportStepRepository).findReadyToDownloadTransportSteps(BACKEND_NAME);
            verifyNoMoreInteractions(transportStepRepository);
        }

        @Test
        void should_return_an_empty_list_when_no_step_is_ready_to_download() {
            when(transportStepRepository.findReadyToDownloadTransportSteps(BACKEND_NAME))
                .thenReturn(List.of());

            var result = service.execute(BACKEND_NAME);

            assertThat(result).isEmpty();
            verify(transportStepRepository).findReadyToDownloadTransportSteps(BACKEND_NAME);
            verifyNoMoreInteractions(transportStepRepository);
        }

        @Test
        void should_only_query_the_repository_with_the_given_backend_name() {
            when(transportStepRepository.findReadyToDownloadTransportSteps("other_backend"))
                .thenReturn(List.of(step("step-010")));

            var result = service.execute("other_backend");

            assertThat(result).hasSize(1);
            verify(transportStepRepository, never())
                .findReadyToDownloadTransportSteps(BACKEND_NAME);
        }
    }

    @Nested
    @DisplayName("when the repository fails")
    class WhenRepositoryFails {
        @Test
        void should_propagate_the_exception_thrown_by_the_repository() {
            when(transportStepRepository.findReadyToDownloadTransportSteps(BACKEND_NAME))
                .thenThrow(new IllegalStateException("database unavailable"));

            assertThatThrownBy(() -> service.execute(BACKEND_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
        }
    }
}
