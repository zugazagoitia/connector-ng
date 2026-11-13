/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.pmode;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import eu.ecodex.connector.ActionTestFixtures;
import eu.ecodex.connector.BusinessDomainIdentifierTestFixtures;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeActionRepository;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeAction;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.pmode.ConnectorProcessingModeActionEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.pmode.ConnectorProcessingModeEntity;
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

@DisplayName("ConnectorActionRepository")
@SuppressWarnings({"checkstyle:MissingJavadocType", "DataFlowIssue", "checkstyle:LineLength"})
public class ConnectorDBProcessingModeActionRepositoryTest extends AbstractRepositoryTest {
    private static final ConnectorBusinessDomainIdentifier DEFAULT_BUSINESS_DOMAIN =
        BusinessDomainIdentifierTestFixtures.createDefaultBusinessDomainIdentifier();
    @Autowired
    private ConnectorProcessingModeActionRepository repository;

    /**
     * Reference data (business domain + processing mode), without actions.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
    })
    private @interface WithProcessingModeData {
    }

    /**
     * Reference data plus seeded actions.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-action.sql",
    })
    private @interface WithActionData {
    }

    @Nested
    @DisplayName("save all")
    class SaveAll {
        @Test
        @WithProcessingModeData
        void should_save_all_the_actions() {
            var action = ActionTestFixtures.createAction();

            var savedAction = repository.saveAll(List.of(action), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedAction).isNotNull();
            assertThat(savedAction).hasSize(1);
        }

        @Test
        @WithProcessingModeData
        void should_persist_the_action_name() {
            var action = ActionTestFixtures.createAction();

            var savedActions = repository.saveAll(List.of(action), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedActions)
                .extracting(ConnectorProcessingModeAction::name)
                .containsExactly(action.name());
        }

        @Test
        @WithProcessingModeData
        void should_return_an_empty_list_when_there_are_no_actions_to_save() {
            var savedActions = repository.saveAll(List.of(), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedActions).isEmpty();
        }

        @Test
        void should_fail_when_the_actions_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.saveAll(null, DEFAULT_BUSINESS_DOMAIN)
            );
        }

        @Test
        void should_fail_when_the_business_domain_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.saveAll(List.of(ActionTestFixtures.createAction()), null)
            );
        }

        @Test
        void should_fail_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.saveAll(null, null)
            );
        }
    }

    @Nested
    @DisplayName("find by name and business domain")
    class FindByNameAndBusinessDomain {
        @Test
        @WithActionData
        void should_find_the_action() {
            var action =
                repository.findByNameAndBusinessDomain("Test_Form", DEFAULT_BUSINESS_DOMAIN);

            assertThat(action).isNotNull();
            assertThat(action.name()).isEqualTo("Test_Form");
        }

        @Test
        @WithActionData
        void should_return_null_when_the_action_name_is_unknown() {
            var action =
                repository.findByNameAndBusinessDomain(
                    "Test_Form_Unknown",
                    DEFAULT_BUSINESS_DOMAIN
                );

            assertThat(action).isNull();
        }

        @Test
        void should_fail_when_the_name_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByNameAndBusinessDomain(null, DEFAULT_BUSINESS_DOMAIN)
            );
        }

        @Test
        void should_fail_when_the_business_domain_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByNameAndBusinessDomain("Test_Form", null)
            );
        }

        @Test
        void should_fail_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByNameAndBusinessDomain(null, null)
            );
        }
    }

    @Nested
    @DisplayName("find all by business domain identifier")
    class FindAllByBusinessDomainIdentifier {
        @Test
        @WithActionData
        void should_find_all_the_actions() {
            var actions = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN);

            assertThat(actions).isNotNull();
            assertThat(actions).hasSize(31);
        }

        @Test
        @WithProcessingModeData
        void should_return_an_empty_list_when_there_are_no_actions() {
            var actions = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN);

            assertThat(actions).isEmpty();
        }

        @Test
        @WithActionData
        void should_return_the_actions_sorted_by_name_ascending() {
            var names = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN)
                                  .stream()
                                  .map(ConnectorProcessingModeAction::name)
                                  .toList();

            assertThat(names.getFirst()).isEqualTo("BD-009001-ChangeAssistance");
            assertThat(names).containsSubsequence(
                "BD-009001-ChangeAssistance",
                "BD-009001-IssueAssistance",
                "BD-009001-TakeMeasure"
            );
        }

        @Test
        void should_fail_when_the_business_domain_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findAllByBusinessDomainIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("to domain")
    class ToDomain {
        @Test
        void should_return_null_when_the_entity_is_null() {
            assertThat(ConnectorDBProcessingModeActionRepository.toDomain(null)).isNull();
        }

        @Test
        void should_map_the_name_of_the_entity() {
            var entity = ConnectorProcessingModeActionEntity
                .builder()
                .name("Test_Form")
                .build();

            var action = ConnectorDBProcessingModeActionRepository.toDomain(entity);

            assertThat(action).isNotNull();
            assertThat(action.name()).isEqualTo("Test_Form");
        }
    }

    @Nested
    @DisplayName("to entity")
    class ToEntity {
        @Test
        void should_map_the_name_and_the_processing_mode() {
            var processingMode = mock(ConnectorProcessingModeEntity.class);
            var action = ActionTestFixtures.createAction();

            var entity =
                ConnectorDBProcessingModeActionRepository.toEntity(action, processingMode);

            assertThat(entity.getName()).isEqualTo(action.name());
            assertThat(entity.getProcessingMode()).isSameAs(processingMode);
        }
    }
}
