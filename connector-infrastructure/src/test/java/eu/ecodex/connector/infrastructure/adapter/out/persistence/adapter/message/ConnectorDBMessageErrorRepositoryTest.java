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
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ecodex.connector.application.port.spi.message.ConnectorMessageErrorRepository;
import eu.ecodex.connector.domain.model.message.ConnectorMessageError;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageErrorEntity;
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

@DisplayName("ConnectorMessageErrorRepository")
public class ConnectorDBMessageErrorRepositoryTest extends AbstractRepositoryTest {
    private static final String MESSAGE_ID =
        "7a169fa8-1f0d-4a2c-aade-796b0b02fe58@connector.ecodex.eu";

    @Autowired
    private ConnectorMessageErrorRepository repository;

    /**
     * Reference data.
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

    @Nested
    @DisplayName("save")
    class Save {
        @Test
        @WithMessageData
        void should_save_the_errors() {
            var savedErrors = repository.save(MESSAGE_ID, errors());

            assertThat(savedErrors).isNotNull();
            assertThat(savedErrors).hasSize(1);
            assertThat(savedErrors.getFirst().label()).isEqualTo("error-label");
        }

        @Test
        @WithMessageData
        void should_persist_all_the_error_fields() {
            var savedErrors = repository.save(MESSAGE_ID, errors());

            assertThat(savedErrors).hasSize(1);
            assertThat(savedErrors.getFirst().label()).isEqualTo("error-label");
            assertThat(savedErrors.getFirst().details()).isEqualTo("error-details");
            assertThat(savedErrors.getFirst().source()).isEqualTo("error-source");
        }

        @Test
        @WithMessageData
        void should_save_all_the_errors_and_keep_their_order() {
            var errors = List.of(
                error("first-error"),
                error("second-error"),
                error("third-error")
            );

            var savedErrors = repository.save(MESSAGE_ID, errors);

            assertThat(savedErrors)
                .extracting(ConnectorMessageError::label)
                .containsExactly("first-error", "second-error", "third-error");
        }

        @Test
        void should_throw_when_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null, List.of())
            );
        }

        @Test
        void should_throw_when_the_errors_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save("message-identifier", null)
            );
        }

        @Test
        void should_throw_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null, null)
            );
        }

        @Test
        void should_throw_when_the_errors_are_empty() {
            assertThrows(
                IllegalArgumentException.class,
                () -> repository.save("message-identifier", List.of())
            );
        }

        @Test
        @WithMessageData
        void should_throw_when_the_errors_are_empty_for_an_existing_message() {
            assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(MESSAGE_ID, List.of())
            );
        }
    }

    @Nested
    @DisplayName("to domain")
    class ToDomain {
        @Test
        void should_map_all_the_fields_of_the_entity() {
            var entity = ConnectorMessageErrorEntity
                .builder()
                .label("error-label")
                .details("error-details")
                .source("error-source")
                .build();

            var error = ConnectorDBMessageErrorRepository.toDomain(entity);

            assertThat(error).isNotNull();
            assertThat(error.label()).isEqualTo("error-label");
            assertThat(error.details()).isEqualTo("error-details");
            assertThat(error.source()).isEqualTo("error-source");
        }

        @Test
        void should_fail_when_the_entity_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> ConnectorDBMessageErrorRepository.toDomain(null)
            );
        }
    }

    private List<ConnectorMessageError> errors() {
        return List.of(error("error-label"));
    }

    private ConnectorMessageError error(String label) {
        return ConnectorMessageError.builder()
                                    .label(label)
                                    .details("error-details")
                                    .source("error-source")
                                    .build();
    }
}
