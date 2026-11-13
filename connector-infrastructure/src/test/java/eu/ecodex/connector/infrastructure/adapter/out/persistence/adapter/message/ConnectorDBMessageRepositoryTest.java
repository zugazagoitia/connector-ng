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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ecodex.connector.BusinessMessageTestFixtures;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageRepository;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.ConnectorMessageJpaRepository;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@SuppressWarnings("DataFlowIssue")

@DisplayName("ConnectorMessageRepository")
public class ConnectorDBMessageRepositoryTest extends AbstractRepositoryTest {
    private static final String MESSAGE_ID = "fd2f35e0-1981-4d21-b718-10a802e884b0@connector"
        + ".ecodex.eu";

    @Autowired
    private ConnectorMessageRepository repository;
    @Autowired
    private ConnectorMessageJpaRepository jpaRepository;

    /**
     * Reference data only (no message rows). Used by the save flow.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-service.sql",
        "classpath:sql/processing-mode-action.sql",
    })
    private @interface WithReferenceData {
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
        "classpath:sql/attachment.sql",
        "classpath:sql/message-business-content.sql",
        "classpath:sql/message-business-document.sql",
        "classpath:sql/evidence.sql",
    })
    private @interface WithMessageData {
    }

    @Nested
    @DisplayName("save a message")
    class Save {
        @Test
        void should_fail_when_the_message_is_null() {
            assertThrows(
                NullPointerException.class, () -> repository.save(null)
            );
        }

        @Test
        @WithReferenceData
        @Sql({
            "classpath:sql/attachment2.sql",
        })
        void should_save_the_message_successfully() {
            var message = BusinessMessageTestFixtures.createOutboundMessage();

            var saved = repository.save(message);

            assertThat(saved).isNotNull();
            assertThat(saved.identifier()).isNotNull();
            assertThat(saved.as4Properties()).isNotNull();
            assertThat(saved.businessContent()).isNotNull();
            assertThat(saved.direction()).isNotNull();
        }
    }

    @Nested
    @DisplayName("update the gateway name")
    class UpdateGatewayName {
        @Test
        @WithMessageData
        void should_update_the_gateway_name_successfully() {
            var message = jpaRepository.findByIdentifier(MESSAGE_ID);
            assertThat(message).isNotNull();
            assertThat(message.getGatewayName()).isEqualTo("default_gateway");

            var update = repository.updateGatewayName(MESSAGE_ID, "gateway-name");

            assertThat(update).isNotNull();
            assertThat(update.gatewayName()).isEqualTo("gateway-name");
        }

        @Test
        void should_fail_if_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateGatewayName(null, "gateway-name")
            );
        }

        @Test
        void should_fail_if_the_gateway_name_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateGatewayName(MESSAGE_ID, null)
            );
        }

        @Test
        void should_fail_if_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateGatewayName(null, null)
            );
        }
    }

    @Nested
    @DisplayName("update the backend name")
    class UpdateBackendName {
        @Test
        @WithMessageData
        void should_update_the_backend_name_successfully() {
            var message = jpaRepository.findByIdentifier(MESSAGE_ID);
            assertThat(message).isNotNull();
            assertThat(message.getGatewayName()).isEqualTo("default_gateway");

            var update = repository.updateBackendName(MESSAGE_ID, "backend-name");

            assertThat(update).isNotNull();
            assertThat(update.backendName()).isEqualTo("backend-name");
        }

        @Test
        void should_fail_if_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateBackendName(null, "backend-name")
            );
        }

        @Test
        void should_fail_if_the_backend_name_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateBackendName(MESSAGE_ID, null)
            );
        }

        @Test
        void should_fail_if_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateBackendName(null, null)
            );
        }
    }

    @Nested
    @DisplayName("update the ebms identifier")
    class UpdateEbmsIdentifier {
        @Test
        @WithMessageData
        void should_update_the_ebms_identifier_successfully() {
            var message = jpaRepository.findByIdentifier(MESSAGE_ID);
            assertThat(message).isNotNull();
            assertThat(message.getAs4Properties().getEbmsMessageIdentifier()).isNull();

            var ebmsIdentifier = String.format("%s@%s", UUID.randomUUID(), "connector.ecodex.eu");

            var update = repository.updateEbmsIdentifier(MESSAGE_ID, ebmsIdentifier);

            assertThat(update).isNotNull();
            assertThat(update.as4Properties().ebmsMessageIdentifier()).isEqualTo(ebmsIdentifier);
        }

        @Test
        void should_fail_if_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateEbmsIdentifier(null, "ebms-identifier")
            );
        }

        @Test
        void should_fail_if_the_ebms_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateEbmsIdentifier(MESSAGE_ID, null)
            );
        }

        @Test
        void should_fail_if_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateEbmsIdentifier(null, null)
            );
        }
    }

    @Nested
    @DisplayName("set a message as rejected")
    class SetAsRejected {
        @Test
        @WithMessageData
        void should_set_the_message_as_rejected_successfully() {
            var message = jpaRepository.findByIdentifier(MESSAGE_ID);
            assertThat(message).isNotNull();
            assertThat(message.getRejectedAt()).isNull();

            var update = repository.setAsRejected(message.getIdentifier());

            assertThat(update).isNotNull();
            assertThat(update.rejectedAt()).isNotNull();
            assertThat(update.status()).isEqualTo(ConnectorBusinessMessageStatus.REJECTED);
        }

        @Test
        void should_fail_if_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.setAsRejected(null)
            );
        }
    }

    @Nested
    @DisplayName("set a message as confirmed")
    class SetAsConfirmed {
        @Test
        @WithMessageData
        void should_set_the_message_as_confirmed_successfully() {
            var message = jpaRepository.findByIdentifier(MESSAGE_ID);
            assertThat(message).isNotNull();
            assertThat(message.getConfirmedAt()).isNull();

            var update = repository.setAsConfirmed(message.getIdentifier());

            assertThat(update).isNotNull();
            assertThat(update.confirmedAt()).isNotNull();
            assertThat(update.status()).isEqualTo(ConnectorBusinessMessageStatus.CONFIRMED);
        }

        @Test
        void should_fail_if_the_message_identifier_is_null() {
            // NOTE: original called setAsRejected(null) here — fixed to setAsConfirmed(null)
            assertThrows(
                NullPointerException.class,
                () -> repository.setAsConfirmed(null)
            );
        }
    }

    @Nested
    @DisplayName("set as deleted")
    class SetAsDeleted {
        @Test
        @WithMessageData
        void should_set_the_deletion_date_on_the_message() {
            repository.setAsDeleted(MESSAGE_ID);

            assertThat(deletedAtOfMessage()).isNotNull();
        }

        @Test
        @WithMessageData
        void should_keep_the_original_deletion_date_when_called_twice() {
            repository.setAsDeleted(MESSAGE_ID);
            var first = deletedAtOfMessage();

            repository.setAsDeleted(MESSAGE_ID);

            assertThat(deletedAtOfMessage()).isEqualTo(first);
        }

        @Test
        void should_do_nothing_when_the_message_does_not_exist() {
            assertThatCode(() -> repository.setAsDeleted("unknown"))
                .doesNotThrowAnyException();
        }

        private Instant deletedAtOfMessage() {
            var timestamp = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM connector_messages WHERE identifier = ?",
                Timestamp.class,
                MESSAGE_ID
            );
            return timestamp == null ? null : timestamp.toInstant();
        }
    }

    @Nested
    @DisplayName("set delivered to link partner at")
    class SetDeliveredToLinkPartnerAt {
        @Test
        @WithMessageData
        void should_set_the_delivered_to_link_partner_timestamp_successfully() {
            var message = jpaRepository.findByIdentifier(MESSAGE_ID);
            assertThat(message).isNotNull();
            assertThat(message.getDeliveredToLinkPartnerAt()).isNull();

            var update = repository.setDeliveredToLinkPartnerAt(message.getIdentifier());

            assertThat(update).isNotNull();
            assertThat(update.deliveredToLinkPartnerAt()).isNotNull();
        }

        @Test
        void should_fail_if_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.setDeliveredToLinkPartnerAt(null)
            );
        }
    }

    @Nested
    @DisplayName("find by conversation identifier")
    class FindByConversationIdentifier {
        @Test
        @WithMessageData
        void should_find_the_matching_messages() {
            var messages = repository.findByConversationIdentifier(
                "5abe51ce-e94a-4df6-8b77-41b10bc47da7");

            assertThat(messages).isNotNull();
            assertThat(messages).hasSize(1);
        }

        @Test
        void should_return_an_empty_list_when_the_conversation_identifier_is_unknown() {
            var messages = repository.findByConversationIdentifier(
                "unknown-conversation-identifier");

            assertThat(messages).isNotNull();
            assertThat(messages).isEmpty();
        }

        @Test
        void should_fail_if_the_conversation_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByConversationIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("find all unresolved")
    class FindAllUnresolved {
        private static final String CONFIRMED_IDENTIFIER =
            "fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu";
        private static final String REJECTED_IDENTIFIER =
            "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu";

        @Test
        @WithMessageData
        void should_return_the_unresolved_messages_created_before_the_threshold() {
            var threshold = Instant.now().plus(Duration.ofDays(1));

            var messages = repository.findAllUnresolved(threshold);

            assertThat(messages)
                .extracting(ConnectorBusinessMessage::identifier)
                .containsExactlyInAnyOrder(
                    "fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu",
                    "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu",
                    "7a169fa8-1f0d-4a2c-aade-796b0b02fe58@connector.ecodex.eu",
                    "3fae4358-7cc9-4929-a17b-4432cbb8b9cc@connector.ecodex.eu"
                );
        }

        @Test
        @WithMessageData
        void should_not_return_the_messages_created_after_the_threshold() {
            var threshold = Instant.now().minus(Duration.ofDays(1));

            var messages = repository.findAllUnresolved(threshold);

            assertThat(messages).isEmpty();
        }

        @Test
        @WithMessageData
        void should_not_return_a_confirmed_message() {
            var threshold = Instant.now().plus(Duration.ofDays(1));
            repository.setAsConfirmed(CONFIRMED_IDENTIFIER);

            var messages = repository.findAllUnresolved(threshold);

            assertThat(messages)
                .extracting(ConnectorBusinessMessage::identifier)
                .hasSize(3)
                .doesNotContain(CONFIRMED_IDENTIFIER);
        }

        @Test
        @WithMessageData
        void should_not_return_a_rejected_message() {
            var threshold = Instant.now().plus(Duration.ofDays(1));
            repository.setAsRejected(REJECTED_IDENTIFIER);

            var messages = repository.findAllUnresolved(threshold);

            assertThat(messages)
                .extracting(ConnectorBusinessMessage::identifier)
                .hasSize(3)
                .doesNotContain(REJECTED_IDENTIFIER);
        }

        @Test
        @WithMessageData
        void should_return_no_message_when_all_the_messages_are_resolved() {
            var threshold = Instant.now().plus(Duration.ofDays(1));
            repository.setAsConfirmed(CONFIRMED_IDENTIFIER);
            repository.setAsRejected(REJECTED_IDENTIFIER);
            repository.setAsConfirmed("7a169fa8-1f0d-4a2c-aade-796b0b02fe58@connector.ecodex.eu");
            repository.setAsRejected("3fae4358-7cc9-4929-a17b-4432cbb8b9cc@connector.ecodex.eu");

            var messages = repository.findAllUnresolved(threshold);

            assertThat(messages).isEmpty();
        }

        @Test
        void should_return_an_empty_list_when_there_is_no_message() {
            var messages = repository.findAllUnresolved(Instant.now().plus(Duration.ofDays(1)));

            assertThat(messages).isEmpty();
        }
    }

    @Nested
    @DisplayName("find all by identifiers")
    class FindAllByIdentifier {
        @Test
        @WithMessageData
        void should_find_the_matching_messages() {
            var messages = repository.findAllByIdentifier(
                List.of("7a169fa8-1f0d-4a2c-aade-796b0b02fe58@connector.ecodex.eu")
            );

            assertThat(messages).isNotNull();
            assertThat(messages).hasSize(1);
        }

        @Test
        void should_fail_if_the_identifiers_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findAllByIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("find by identifier")
    class FindByIdentifier {
        @Test
        @WithMessageData
        void should_find_the_message_with_its_attachments_and_evidences() {
            var message = repository.findByIdentifier(
                "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu");

            assertThat(message).isNotNull();
            assertThat(message.identifier()).isEqualTo(
                "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu");
            assertThat(message.direction()).isEqualTo(ConnectorMessageDirection.GATEWAY_TO_BACKEND);
            assertThat(message.attachments()).isNotEmpty();
            assertThat(message.evidences()).isNotEmpty();
            assertThat(message.errors()).isEmpty();
        }

        @Test
        void should_return_null_when_the_identifier_is_unknown() {
            var message = repository.findByIdentifier("unknown-identifier");
            assertThat(message).isNull();
        }

        @Test
        void should_fail_if_the_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("find referenced business message")
    class FindReferencedBusinessMessage {
        @Test
        @WithMessageData
        void should_return_the_referenced_business_message_using_backend_identifier() {
            var message = repository.findReferencedBusinessMessage(
                "1f30e203-f89c-4568-a076-469c4f8b35a5",
                ConnectorMessageDirection.BACKEND_TO_GATEWAY
            );

            assertThat(message).isNotNull();
            assertThat(message.backendMessageIdentifier()).isEqualTo(
                "1f30e203-f89c-4568-a076-469c4f8b35a5");
        }

        @Test
        @WithMessageData
        void should_return_the_referenced_business_message_using_ebms_identifier() {
            var message = repository.findReferencedBusinessMessage(
                "25873d6d-cfa8-4983-b39a-ffe38860872e@connector.ecodex.eu",
                ConnectorMessageDirection.BACKEND_TO_GATEWAY
            );

            assertThat(message).isNotNull();
            assertThat(message.as4Properties().ebmsMessageIdentifier()).isEqualTo(
                "25873d6d-cfa8-4983-b39a-ffe38860872e@connector.ecodex.eu");
        }

        @Test
        @WithMessageData
        void should_return_the_referenced_business_message_using_connector_identifier() {
            var message = repository.findReferencedBusinessMessage(
                "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu",
                ConnectorMessageDirection.BACKEND_TO_GATEWAY
            );

            assertThat(message).isNotNull();
            assertThat(message.identifier()).isEqualTo(
                "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu");
        }

        @Test
        @WithMessageData
        void should_return_null_if_the_referenced_business_message_is_not_found() {
            var message = repository.findReferencedBusinessMessage(
                "non-existing-message@connector.ecodex.eu",
                ConnectorMessageDirection.BACKEND_TO_GATEWAY
            );

            assertThat(message).isNull();
        }
    }

    @Nested
    @DisplayName("update the backend identifier")
    class UpdateBackendIdentifier {
        @Test
        @WithMessageData
        void should_update_the_backend_identifier_successfully() {
            var message = repository.updateBackendIdentifier(
                "7a169fa8-1f0d-4a2c-aade-796b0b02fe58@connector.ecodex.eu", "backend-identifier"
            );

            assertThat(message).isNotNull();
            assertThat(message.backendMessageIdentifier()).isEqualTo("backend-identifier");
        }

        @Test
        void should_fail_if_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateBackendIdentifier(null, "backend-identifier")
            );
        }

        @Test
        void should_fail_if_the_backend_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateBackendIdentifier("message-identifier", null)
            );
        }

        @Test
        void should_fail_if_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateBackendIdentifier(null, null)
            );
        }
    }

    @Nested
    @DisplayName("delete by identifier")
    class DeleteByIdentifier {
        private static final String OTHER_MESSAGE_ID =
            "7a169fa8-1f0d-4a2c-aade-796b0b02fe58@connector.ecodex.eu";

        private int countMessages(String identifier) {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_messages WHERE identifier = ?",
                Integer.class,
                identifier
            );
        }

        private int countAllMessages() {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_messages",
                Integer.class
            );
        }

        private int countRowsOfMessage(String table, String identifier) {
            return jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*) FROM %s t
                      JOIN connector_messages m ON t.message_id = m.id
                     WHERE m.identifier = ?
                    """.formatted(table),
                Integer.class,
                identifier
            );
        }

        private void deleteAndFlush(String identifier) {
            repository.deleteByIdentifier(identifier);
            entityManager.flush();
        }

        @Test
        @WithMessageData
        void should_delete_the_message() {
            assertThat(countMessages(MESSAGE_ID)).isEqualTo(1);

            deleteAndFlush(MESSAGE_ID);

            assertThat(countMessages(MESSAGE_ID)).isZero();
            assertThat(repository.findByIdentifier(MESSAGE_ID)).isNull();
        }

        @Test
        @WithMessageData
        void should_delete_the_as4_properties_of_the_message() {
            assertThat(countRowsOfMessage("connector_message_as4_properties", MESSAGE_ID))
                .isEqualTo(1);

            deleteAndFlush(MESSAGE_ID);

            var remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_message_as4_properties",
                Integer.class
            );
            assertThat(remaining).isEqualTo(3);
        }

        @Test
        @WithMessageData
        void should_delete_the_business_content_of_the_message() {
            var before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_message_business_contents",
                Integer.class
            );

            repository.deleteByIdentifier(MESSAGE_ID);

            var after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_message_business_contents",
                Integer.class
            );
            assertThat(after).isEqualTo(before - 1);
        }

        @Test
        @WithMessageData
        void should_not_delete_the_other_messages() {
            var before = countAllMessages();

            deleteAndFlush(MESSAGE_ID);

            assertThat(countAllMessages()).isEqualTo(before - 1);
            assertThat(repository.findByIdentifier(OTHER_MESSAGE_ID)).isNotNull();
        }

        @Test
        @WithMessageData
        void should_be_idempotent_when_called_twice() {
            repository.deleteByIdentifier(MESSAGE_ID);

            assertThatCode(() -> repository.deleteByIdentifier(MESSAGE_ID))
                .doesNotThrowAnyException();
            assertThat(countMessages(MESSAGE_ID)).isZero();
        }

        @Test
        @WithMessageData
        void should_do_nothing_when_the_message_does_not_exist() {
            var before = countAllMessages();

            assertThatCode(() -> repository.deleteByIdentifier("unknown@connector.ecodex.eu"))
                .doesNotThrowAnyException();

            assertThat(countAllMessages()).isEqualTo(before);
        }

        @Test
        void should_fail_if_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.deleteByIdentifier(null)
            );
        }
    }
}
