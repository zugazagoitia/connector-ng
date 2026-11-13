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

import eu.ecodex.connector.BusinessDomainIdentifierTestFixtures;
import eu.ecodex.connector.ProcessingModeTestFixtures;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeRepository;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.security.ConnectorTruststore;
import eu.ecodex.connector.domain.model.security.KeystoreType;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@DisplayName("ConnectorProcessingModeRepository")
@SuppressWarnings({"checkstyle:MissingJavadocType", "checkstyle:LineLength", "DataFlowIssue"})
public class ConnectorDBProcessingModeRepositoryTest extends AbstractRepositoryTest {
    private static final String PROCESSING_MODE_UUID = "4f10aed9-2e5f-4780-87f7-5fe1070d5ccf";
    private static final ConnectorBusinessDomainIdentifier DEFAULT_BUSINESS_DOMAIN =
        BusinessDomainIdentifierTestFixtures.createDefaultBusinessDomainIdentifier();
    @Autowired
    private ConnectorProcessingModeRepository repository;

    /**
     * Reference data plus a seeded processing mode.
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
     * Reference data plus a seeded processing mode with its parties and actions.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-action.sql",
    })
    private @interface WithPartiesAndActionData {
    }

    /**
     * Builds a truststore. Adjust here if the {@link ConnectorTruststore} field types differ.
     */
    private static ConnectorTruststore createTruststore(String filename, String password) {
        return ConnectorTruststore
            .builder()
            .content(new byte[] {1, 2, 3})
            .password(password)
            .type(KeystoreType.JKS)
            .filename(filename)
            .build();
    }

    @Nested
    @DisplayName("save a processing mode")
    class Save {
        @Test
        @Sql("classpath:sql/business-domain.sql")
        void should_save_the_processing_mode() {
            var processingMode = ProcessingModeTestFixtures.createWithBusinessDomain();

            var savedProcessingMode = repository.save(
                processingMode, processingMode.businessDomain().identifier()
            );

            assertThat(savedProcessingMode).isNotNull();
        }

        @Test
        @Sql("classpath:sql/business-domain.sql")
        void should_persist_the_processing_mode_fields() {
            var processingMode = ProcessingModeTestFixtures.createWithBusinessDomain();

            var savedProcessingMode = repository.save(
                processingMode, processingMode.businessDomain().identifier()
            );

            assertThat(savedProcessingMode.description()).isEqualTo(processingMode.description());
            assertThat(savedProcessingMode.content()).isEqualTo(processingMode.content());
            assertThat(savedProcessingMode.filename()).isEqualTo(processingMode.filename());
        }

        @Test
        @Sql("classpath:sql/business-domain.sql")
        void should_persist_the_truststore() {
            var processingMode = ProcessingModeTestFixtures.createWithBusinessDomain();

            var savedProcessingMode = repository.save(
                processingMode, processingMode.businessDomain().identifier()
            );

            assertThat(savedProcessingMode.truststore())
                .usingRecursiveComparison()
                .isEqualTo(processingMode.truststore());
        }

        @Test
        @Sql("classpath:sql/business-domain.sql")
        void should_make_the_processing_mode_available_through_find_all() {
            var processingMode = ProcessingModeTestFixtures.createWithBusinessDomain();

            repository.save(processingMode, processingMode.businessDomain().identifier());

            assertThat(repository.findAll()).hasSize(1);
        }

        @Test
        void should_fail_when_the_processing_mode_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null, DEFAULT_BUSINESS_DOMAIN)
            );
        }

        @Test
        void should_fail_when_the_business_domain_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(ProcessingModeTestFixtures.createWithBusinessDomain(), null)
            );
        }

        @Test
        void should_fail_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null, null)
            );
        }
    }

    @Nested
    @DisplayName("update the truststore")
    class UpdateTruststore {
        @Test
        @WithProcessingModeData
        void should_create_the_truststore_when_the_processing_mode_has_none() {
            var truststore = createTruststore("truststore.jks", "secret");

            var updated = repository.updateTruststore(PROCESSING_MODE_UUID, truststore);

            assertThat(updated).isNotNull();
            assertThat(updated.truststore())
                .usingRecursiveComparison()
                .isEqualTo(truststore);
        }

        @Test
        @WithProcessingModeData
        void should_replace_the_truststore_when_the_processing_mode_already_has_one() {
            var initial = createTruststore("initial.jks", "initial-secret");
            var replacement = createTruststore("replacement.jks", "replacement-secret");
            repository.updateTruststore(PROCESSING_MODE_UUID, initial);

            var updated = repository.updateTruststore(PROCESSING_MODE_UUID, replacement);

            assertThat(updated.truststore())
                .usingRecursiveComparison()
                .isEqualTo(replacement);
        }

        @Test
        @WithProcessingModeData
        void should_keep_the_processing_mode_data_when_the_truststore_is_updated() {
            var updated = repository.updateTruststore(
                PROCESSING_MODE_UUID,
                createTruststore("truststore.jks", "secret")
            );

            assertThat(updated.uuid()).isEqualTo(PROCESSING_MODE_UUID);
            assertThat(updated.description()).isEqualTo("test processing mode");
            assertThat(updated.filename()).isEqualTo("pmode.xml");
        }

        @Test
        @WithProcessingModeData
        void should_expose_the_new_truststore_when_the_processing_mode_is_found_again() {
            var truststore = createTruststore("truststore.jks", "secret");
            repository.updateTruststore(PROCESSING_MODE_UUID, truststore);

            var found = repository.findByUuid(PROCESSING_MODE_UUID);

            assertThat(found.truststore())
                .usingRecursiveComparison()
                .isEqualTo(truststore);
        }

        @Test
        void should_fail_when_the_uuid_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateTruststore(null, createTruststore("t.jks", "secret"))
            );
        }

        @Test
        void should_fail_when_the_truststore_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateTruststore(PROCESSING_MODE_UUID, null)
            );
        }

        @Test
        void should_fail_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.updateTruststore(null, null)
            );
        }
    }

    @Nested
    @DisplayName("find by uuid")
    class FindByUuid {
        @Test
        @WithProcessingModeData
        void should_find_the_processing_mode() {
            var processingMode = repository.findByUuid(PROCESSING_MODE_UUID);

            assertThat(processingMode).isNotNull();
            assertThat(processingMode.uuid()).isNotNull();
        }

        @Test
        @WithProcessingModeData
        void should_map_all_the_fields_of_the_processing_mode() {
            var processingMode = repository.findByUuid(PROCESSING_MODE_UUID);

            assertThat(processingMode.uuid()).isEqualTo(PROCESSING_MODE_UUID);
            assertThat(processingMode.description()).isEqualTo("test processing mode");
            assertThat(processingMode.filename()).isEqualTo("pmode.xml");
            assertThat(processingMode.businessDomain().identifier())
                .isEqualTo(DEFAULT_BUSINESS_DOMAIN);
            assertThat(processingMode.createdAt()).isNotNull();
            assertThat(processingMode.updatedAt()).isNotNull();
        }

        @Test
        @WithProcessingModeData
        void should_have_no_truststore_nor_children_when_none_are_seeded() {
            var processingMode = repository.findByUuid(PROCESSING_MODE_UUID);

            assertThat(processingMode.truststore()).isNull();
            assertThat(processingMode.parties()).isEmpty();
            assertThat(processingMode.actions()).isEmpty();
        }

        @Test
        @WithPartiesAndActionData
        void should_map_the_parties_and_the_actions() {
            var processingMode = repository.findByUuid(PROCESSING_MODE_UUID);

            assertThat(processingMode.parties()).hasSize(4);
            assertThat(processingMode.actions()).hasSize(31);
        }

        @Test
        void should_return_null_when_the_uuid_is_unknown() {
            var processingMode = repository.findByUuid("unknown-uuid");

            assertThat(processingMode).isNull();
        }

        @Test
        void should_fail_when_the_uuid_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByUuid(null)
            );
        }
    }

    @Nested
    @DisplayName("find by business domain identifier")
    class FindByBusinessDomainIdentifier {
        @Test
        @WithProcessingModeData
        void should_find_the_processing_mode() {
            var processingMode = repository.findByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN);

            assertThat(processingMode).isNotNull();
            assertThat(processingMode.businessDomain().identifier())
                .isEqualTo(DEFAULT_BUSINESS_DOMAIN);
        }

        @Test
        @WithProcessingModeData
        void should_find_the_same_processing_mode_as_by_uuid() {
            var byDomain = repository.findByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN);

            assertThat(byDomain.uuid()).isEqualTo(PROCESSING_MODE_UUID);
        }

        @Test
        @WithProcessingModeData
        void should_return_null_when_the_business_domain_is_unknown() {
            var processingMode = repository.findByBusinessDomainIdentifier(
                ConnectorBusinessDomainIdentifier.builder().build()
            );

            assertThat(processingMode).isNull();
        }

        @Test
        void should_fail_when_the_business_domain_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByBusinessDomainIdentifier(null)
            );
        }
    }

    @Nested
    @DisplayName("find all")
    class FindAll {
        @Test
        @WithProcessingModeData
        void should_find_all_the_processing_modes() {
            var processingModes = repository.findAll();

            assertThat(processingModes).isNotNull();
            assertThat(processingModes).hasSize(1);
        }

        @Test
        void should_return_an_empty_list_when_there_are_no_processing_modes() {
            var processingModes = repository.findAll();

            assertThat(processingModes).isEmpty();
        }
    }
}
