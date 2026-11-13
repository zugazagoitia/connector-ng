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

import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
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
@DisplayName("ConnectorListPendingTransportStepsIdsService")
class ConnectorListPendingTransportStepsIdsServiceTest {
    private static final String BACKEND_NAME = "default_test_backend";

    @Mock
    private ConnectorMessageTransportStepRepository transportStepRepository;

    @InjectMocks
    private ConnectorListPendingTransportStepsIdsService service;

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
        void should_return_the_ids_of_the_steps_ready_to_download_for_the_backend() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(List.of("step-001", "step-002", "step-003"));

            var result = service.execute(BACKEND_NAME);

            assertThat(result).containsExactly("step-001", "step-002", "step-003");
            verify(transportStepRepository).findReadyToDownloadTransportStepsIds(BACKEND_NAME);
            verifyNoMoreInteractions(transportStepRepository);
        }

        @Test
        void should_return_an_empty_list_when_no_step_is_ready_to_download() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenReturn(List.of());

            var result = service.execute(BACKEND_NAME);

            assertThat(result).isEmpty();
            verify(transportStepRepository).findReadyToDownloadTransportStepsIds(BACKEND_NAME);
            verifyNoMoreInteractions(transportStepRepository);
        }

        @Test
        void should_only_query_the_repository_with_the_given_backend_name() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds("other_backend"))
                .thenReturn(List.of("step-010"));

            var result = service.execute("other_backend");

            assertThat(result).containsExactly("step-010");
            verify(transportStepRepository, never())
                .findReadyToDownloadTransportStepsIds(BACKEND_NAME);
        }
    }

    @Nested
    @DisplayName("when the repository fails")
    class WhenRepositoryFails {
        @Test
        void should_propagate_the_exception_thrown_by_the_repository() {
            when(transportStepRepository.findReadyToDownloadTransportStepsIds(BACKEND_NAME))
                .thenThrow(new IllegalStateException("database unavailable"));

            assertThatThrownBy(() -> service.execute(BACKEND_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
        }
    }
}
