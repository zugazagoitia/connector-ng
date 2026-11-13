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
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import eu.ecodex.connector.BusinessDomainIdentifierTestFixtures;
import eu.ecodex.connector.ServiceTestFixtures;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModeServiceRepository;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeService;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.pmode.ConnectorProcessingModeEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.pmode.ConnectorProcessingModeServiceJpaRepository;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

@DisplayName("ConnectorServiceRepository")
@SuppressWarnings({"checkstyle:MissingJavadocType", "DataFlowIssue", "checkstyle:LineLength"})
public class ConnectorDBProcessingModeServiceRepositoryTest extends AbstractRepositoryTest {
    private static final ConnectorBusinessDomainIdentifier DEFAULT_BUSINESS_DOMAIN =
        BusinessDomainIdentifierTestFixtures.createDefaultBusinessDomainIdentifier();
    @Autowired
    private ConnectorProcessingModeServiceRepository repository;
    @MockitoSpyBean
    private ConnectorProcessingModeServiceJpaRepository jpaRepository;

    /**
     * Reference data (business domain + processing mode), without services.
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
     * Reference data plus seeded services.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-service.sql",
    })
    private @interface WithServiceData {
    }

    @Nested
    @DisplayName("save all")
    class SaveAll {
        @Test
        @WithProcessingModeData
        void should_save_all_the_services() {
            var service = ServiceTestFixtures.createService();

            var savedServices = repository.saveAll(List.of(service), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedServices).isNotNull();
            assertThat(savedServices).hasSize(1);

            verify(jpaRepository).saveAll(anyList());
        }

        @Test
        @WithProcessingModeData
        void should_persist_all_the_service_fields() {
            var service = ServiceTestFixtures.createService();

            var savedServices = repository.saveAll(List.of(service), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedServices)
                .usingRecursiveComparison()
                .isEqualTo(List.of(service));
        }

        @Test
        @WithProcessingModeData
        void should_make_the_saved_service_findable_by_name() {
            var service = ServiceTestFixtures.createService();
            repository.saveAll(List.of(service), DEFAULT_BUSINESS_DOMAIN);

            var found = repository.findByNameAndBusinessDomain(
                service.name(), DEFAULT_BUSINESS_DOMAIN
            );

            assertThat(found).isNotNull();
            assertThat(found.name()).isEqualTo(service.name());
        }

        @Test
        @WithProcessingModeData
        void should_return_an_empty_list_when_there_are_no_services_to_save() {
            var savedServices = repository.saveAll(List.of(), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedServices).isEmpty();
        }

        @Test
        void should_fail_when_the_services_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.saveAll(null, DEFAULT_BUSINESS_DOMAIN)
            );
        }

        @Test
        void should_fail_when_the_business_domain_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.saveAll(List.of(ServiceTestFixtures.createService()), null)
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
        @WithServiceData
        void should_find_the_service() {
            var service =
                repository.findByNameAndBusinessDomain("Connector-TEST", DEFAULT_BUSINESS_DOMAIN);

            assertThat(service).isNotNull();
            assertThat(service.name()).isEqualTo("Connector-TEST");
        }

        @Test
        @WithServiceData
        void should_return_null_when_the_service_name_is_unknown() {
            var service =
                repository.findByNameAndBusinessDomain("unknown", DEFAULT_BUSINESS_DOMAIN);

            assertThat(service).isNull();
        }

        @Test
        @WithProcessingModeData
        void should_return_null_when_there_are_no_services() {
            var service =
                repository.findByNameAndBusinessDomain("Connector-TEST", DEFAULT_BUSINESS_DOMAIN);

            assertThat(service).isNull();
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
                () -> repository.findByNameAndBusinessDomain("Connector-TEST", null)
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
        @WithServiceData
        void should_find_all_the_services() {
            var services = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN);

            assertThat(services).isNotNull();
            assertThat(services).hasSize(7);
        }

        @Test
        @WithProcessingModeData
        void should_return_an_empty_list_when_there_are_no_services() {
            var services = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN);

            assertThat(services).isEmpty();
        }

        @Test
        @WithServiceData
        void should_return_the_services_sorted_by_name_ascending() {
            var names = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN)
                                  .stream()
                                  .map(ConnectorProcessingModeService::name)
                                  .toList();

            assertThat(names).isSortedAccordingTo(Comparator.naturalOrder());
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
            assertThat(ConnectorDBProcessingModeServiceRepository.toDomain(null)).isNull();
        }

        @Test
        void should_map_all_the_fields_of_the_entity() {
            var service = ServiceTestFixtures.createService();
            var entity = ConnectorDBProcessingModeServiceRepository.toEntity(
                service, mock(ConnectorProcessingModeEntity.class)
            );

            var mapped = ConnectorDBProcessingModeServiceRepository.toDomain(entity);

            assertThat(mapped)
                .usingRecursiveComparison()
                .isEqualTo(service);
        }
    }

    @Nested
    @DisplayName("to entity")
    class ToEntity {
        @Test
        void should_map_all_the_fields_and_the_processing_mode() {
            var processingMode = mock(ConnectorProcessingModeEntity.class);
            var service = ServiceTestFixtures.createService();

            var entity = ConnectorDBProcessingModeServiceRepository.toEntity(
                service, processingMode
            );

            assertThat(entity.getName()).isEqualTo(service.name());
            assertThat(entity.getType()).isEqualTo(service.type());
            assertThat(entity.getProcessingMode()).isSameAs(processingMode);
        }
    }
}
