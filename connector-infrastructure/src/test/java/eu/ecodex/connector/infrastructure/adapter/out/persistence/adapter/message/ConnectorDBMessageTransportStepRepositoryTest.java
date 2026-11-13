/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.message;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStepStatus;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@SuppressWarnings("DataFlowIssue")
@DisplayName("ConnectorDBMessageTransportStepRepository")
public class ConnectorDBMessageTransportStepRepositoryTest extends AbstractRepositoryTest {
    private static final String STEP_IDENTIFIER =
        "8af8af19-839a-4594-a19d-40d67868474c@connector.ecodex.eu_backend_alice";
    private static final String MESSAGE_IDENTIFIER =
        "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu";
    private static final String PENDING_STEP_IDENTIFIER =
        "b0f19c4c-ac3e-438c-9951-8e3a5211fed4@connector.ecodex.eu_backend_alice";
    private static final String BACKEND_NAME = "backend_alice";

    @Autowired
    private ConnectorMessageTransportStepRepository repository;

    private ConnectorMessageTransportStep generateTransportStep() {
        return generateTransportStep(null);
    }

    private ConnectorMessageTransportStep generateTransportStep(
        String transportedMessageIdentifier) {
        return ConnectorMessageTransportStep
            .builder()
            .identifier(STEP_IDENTIFIER)
            .numberOfAttempts(0)
            .linkPartnerName("default_test_backend")
            .transportedMessageIdentifier(transportedMessageIdentifier)
            .status(ConnectorMessageTransportStatus.READY_FOR_PULL)
            .transportedMessage(
                BusinessMessageTestFixtures.createInboundMessage()
                                           .toBuilder()
                                           .identifier(MESSAGE_IDENTIFIER)
                                           .build()
            )
            .build();
    }

    /**
     * Reference data plus a seeded message and its AS4 properties.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-service.sql",
        "classpath:sql/processing-mode-action.sql",
        "classpath:sql/message.sql",
        "classpath:sql/message-as4-properties.sql",
    })
    private @interface WithMessageData {
    }

    /**
     * Message data plus seeded transport steps and their statuses.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-service.sql",
        "classpath:sql/processing-mode-action.sql",
        "classpath:sql/message.sql",
        "classpath:sql/message-as4-properties.sql",
        "classpath:sql/message-transport-step.sql",
        "classpath:sql/message-transport-step-statuses.sql",
    })
    private @interface WithTransportStepData {
    }

    @Nested
    @DisplayName("save a transport step")
    class Save {
        @Test
        void should_throw_when_the_transport_step_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null)
            );
        }

        @Test
        @WithMessageData
        void should_create_a_new_transport_step() {
            var transportStep = repository.save(generateTransportStep());

            assertThat(transportStep).isNotNull();
            assertThat(transportStep.identifier()).isEqualTo(STEP_IDENTIFIER);
        }

        @Test
        @WithMessageData
        void should_persist_the_transport_step_fields() {
            var transportStep = repository.save(generateTransportStep());

            assertThat(transportStep.numberOfAttempts()).isEqualTo(0);
            assertThat(transportStep.linkPartnerName()).isEqualTo("default_test_backend");
            assertThat(transportStep.status())
                .isEqualTo(ConnectorMessageTransportStatus.READY_FOR_PULL);
        }

        @Test
        @WithMessageData
        void should_persist_the_transported_message() {
            var transportStep = repository.save(generateTransportStep());

            assertThat(transportStep.transportedMessage())
                .isNotNull()
                .isInstanceOf(ConnectorBusinessMessage.class);
        }

        @Test
        @WithMessageData
        void should_record_the_initial_status_in_the_status_history() {
            var transportStep = repository.save(generateTransportStep());

            assertThat(transportStep.statuses())
                .extracting(ConnectorMessageTransportStepStatus::status)
                .containsExactly(ConnectorMessageTransportStatus.READY_FOR_PULL);
        }

        @Test
        @WithMessageData
        void should_make_the_saved_transport_step_findable_by_identifier() {
            repository.save(generateTransportStep());

            var found = repository.findByIdentifier(STEP_IDENTIFIER);

            assertThat(found).isNotNull();
            assertThat(found.identifier()).isEqualTo(STEP_IDENTIFIER);
        }
    }

    @Nested
    @DisplayName("update a transport step")
    class Update {
        @Test
        void should_throw_when_the_transport_step_status_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.update(STEP_IDENTIFIER, null, 2)
            );
        }

        @Test
        void should_throw_when_the_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.update(null, ConnectorMessageTransportStatus.DELIVERED, 2)
            );
        }

        @Test
        void should_throw_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.update(null, null, 1)
            );
        }

        @Test
        void should_throw_when_no_transport_step_matches_the_identifier() {
            var exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.update(
                    "unknown-identifier",
                    ConnectorMessageTransportStatus.PULLED,
                    1
                )
            );

            assertThat(exception.getMessage()).contains("unknown-identifier");
        }

        @Test
        @WithTransportStepData
        void should_update_an_existing_transport_step() {
            var updatedStep = repository.update(
                STEP_IDENTIFIER,
                ConnectorMessageTransportStatus.PULLED,
                2
            );

            assertThat(updatedStep).isNotNull();
            assertThat(updatedStep.identifier()).isEqualTo(STEP_IDENTIFIER);
            assertThat(updatedStep.statuses()).hasSize(2);
        }

        @Test
        @WithTransportStepData
        void should_update_the_status_and_the_number_of_attempts() {
            var updatedStep = repository.update(
                STEP_IDENTIFIER,
                ConnectorMessageTransportStatus.PULLED,
                2
            );

            assertThat(updatedStep.status()).isEqualTo(ConnectorMessageTransportStatus.PULLED);
            assertThat(updatedStep.numberOfAttempts()).isEqualTo(2);
        }

        @Test
        @WithTransportStepData
        void should_add_the_new_status_to_the_status_history() {
            var updatedStep = repository.update(
                STEP_IDENTIFIER,
                ConnectorMessageTransportStatus.PULLED,
                2
            );

            assertThat(updatedStep.statuses())
                .extracting(ConnectorMessageTransportStepStatus::status)
                .contains(ConnectorMessageTransportStatus.PULLED);
        }
    }

    @Nested
    @DisplayName("update the status")
    class UpdateStatus {
        @Test
        void should_throw_when_the_identifiers_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateStatus(null, ConnectorMessageTransportStatus.PULLED)
            );
        }

        @Test
        void should_throw_when_the_status_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateStatus(List.of(PENDING_STEP_IDENTIFIER), null)
            );
        }

        @Test
        void should_throw_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateStatus(null, null)
            );
        }

        @Test
        @WithTransportStepData
        void should_update_the_status() {
            repository.updateStatus(
                List.of(PENDING_STEP_IDENTIFIER),
                ConnectorMessageTransportStatus.PULLED
            );

            var updatedTransportStep = repository.findByIdentifier(PENDING_STEP_IDENTIFIER);

            assertThat(updatedTransportStep).isNotNull();
            assertThat(updatedTransportStep.status())
                .isEqualTo(ConnectorMessageTransportStatus.PULLED);
            assertThat(updatedTransportStep.statuses()).hasSize(2);
        }

        @Test
        @WithTransportStepData
        void should_not_change_the_transport_steps_that_are_not_targeted() {
            var statusBefore = repository.findByIdentifier(STEP_IDENTIFIER).status();

            repository.updateStatus(
                List.of(PENDING_STEP_IDENTIFIER),
                ConnectorMessageTransportStatus.PULLED
            );

            var statusAfter = repository.findByIdentifier(STEP_IDENTIFIER).status();
            assertThat(statusAfter).isEqualTo(statusBefore);
        }
    }

    @Nested
    @DisplayName("find by identifier")
    class FindByIdentifier {
        @Test
        @WithTransportStepData
        void should_find_the_transport_step() {
            var found = repository.findByIdentifier(STEP_IDENTIFIER);

            assertThat(found).isNotNull();
            assertThat(found.identifier()).isEqualTo(STEP_IDENTIFIER);
            assertThat(found.statuses()).isNotEmpty();
            assertThat(found.transportedMessage()).isNotNull();
        }

        @Test
        void should_return_null_when_the_identifier_is_unknown() {
            var found = repository.findByIdentifier("unknown-identifier");

            assertThat(found).isNull();
        }

        @Test
        void should_throw_when_the_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("find by step, message or remote system identifier")
    class FindByStepOrMessageOrRemoteSystemIdentifier {
        @Test
        @WithTransportStepData
        void should_find_the_transport_step_by_its_own_identifier() {
            var found = repository.findByStepOrMessageOrRemoteSystemIdentifier(STEP_IDENTIFIER);

            assertThat(found).isNotNull();
            assertThat(found.identifier()).isEqualTo(STEP_IDENTIFIER);
        }

        @Test
        void should_return_null_when_the_identifier_is_unknown() {
            var found = repository.findByStepOrMessageOrRemoteSystemIdentifier(
                "unknown-identifier"
            );

            assertThat(found).isNull();
        }

        @Test
        void should_throw_when_the_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByStepOrMessageOrRemoteSystemIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("find pending message ids")
    class FindPendingMessagesIds {
        @Test
        void should_throw_when_the_backend_name_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findReadyToDownloadTransportSteps(null)
            );
        }

        @Test
        @WithTransportStepData
        void should_find_the_pending_message_ids() {
            var messagesIds = repository.findReadyToDownloadTransportSteps(BACKEND_NAME);

            assertThat(messagesIds).isNotNull();
            assertThat(messagesIds).hasSize(1);
        }

        @Test
        @WithTransportStepData
        void should_only_return_the_steps_that_are_ready_for_pull() {
            var steps = repository.findReadyToDownloadTransportSteps(BACKEND_NAME);

            assertThat(steps)
                .extracting(ConnectorMessageTransportStep::status)
                .containsOnly(ConnectorMessageTransportStatus.READY_FOR_PULL);
        }

        @Test
        @WithTransportStepData
        void should_return_an_empty_list_when_the_backend_is_unknown() {
            var steps = repository.findReadyToDownloadTransportSteps("unknown_backend");

            assertThat(steps).isEmpty();
        }
    }

    @Nested
    @DisplayName("find ready to download transport step ids")
    class FindReadyToDownloadTransportStepsIds {
        @Test
        void should_throw_when_the_backend_name_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findReadyToDownloadTransportStepsIds(null)
            );
        }

        @Test
        @WithTransportStepData
        void should_return_the_identifiers_of_the_ready_transport_steps() {
            var ids = repository.findReadyToDownloadTransportStepsIds(BACKEND_NAME);
            var expectedIds = repository.findReadyToDownloadTransportSteps(BACKEND_NAME)
                                        .stream()
                                        .map(ConnectorMessageTransportStep::identifier)
                                        .toList();

            assertThat(ids).isNotNull();
            assertThat(ids).containsExactlyInAnyOrderElementsOf(expectedIds);
        }

        @Test
        @WithTransportStepData
        void should_return_an_empty_list_when_the_backend_is_unknown() {
            var ids = repository.findReadyToDownloadTransportStepsIds("unknown_backend");

            assertThat(ids).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete by transported message identifier")
    class DeleteByTransportedMessageIdentifier {
        @Test
        @WithMessageData
        void should_delete_the_transport_steps_of_the_message() {
            repository.save(generateTransportStep(MESSAGE_IDENTIFIER));
            assertThat(repository.findByIdentifier(STEP_IDENTIFIER)).isNotNull();

            repository.deleteByTransportedMessageIdentifier(MESSAGE_IDENTIFIER);

            assertThat(repository.findByIdentifier(STEP_IDENTIFIER)).isNull();
        }

        @Test
        @WithTransportStepData
        void should_not_fail_when_the_message_has_no_transport_steps() {
            assertDoesNotThrow(
                () -> repository.deleteByTransportedMessageIdentifier("unknown-message")
            );
        }

        @Test
        void should_throw_when_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.deleteByTransportedMessageIdentifier(null)
            );
        }
    }
}
