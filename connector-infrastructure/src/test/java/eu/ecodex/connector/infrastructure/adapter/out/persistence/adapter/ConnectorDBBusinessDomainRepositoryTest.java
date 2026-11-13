/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ecodex.connector.BusinessDomainTestFixtures;
import eu.ecodex.connector.application.port.spi.ConnectorBusinessDomainRepository;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomain;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.ConnectorBusinessDomainEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("ConnectorBusinessDomainRepository")
public class ConnectorDBBusinessDomainRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private ConnectorBusinessDomainRepository repository;

    /**
     * Builds a business domain with the given lane identifier, reusing the default fixture for the
     * other fields.
     */
    private static ConnectorBusinessDomain domainWithIdentifier(
        String messageLaneIdentifier,
        boolean enabled) {
        var defaultDomain = BusinessDomainTestFixtures.createDefaultBusinessDomain();

        return ConnectorBusinessDomain
            .builder()
            .identifier(
                ConnectorBusinessDomainIdentifier
                    .builder()
                    .messageLaneIdentifier(messageLaneIdentifier)
                    .build()
            )
            .description(defaultDomain.description())
            .source(defaultDomain.source())
            .enabled(enabled)
            .build();
    }

    @Nested
    @DisplayName("save a business domain")
    class Save {
        @Test
        void should_save_the_business_domain() {
            var domain = BusinessDomainTestFixtures.createDefaultBusinessDomain();

            var savedDomain = repository.save(domain);

            assertThat(savedDomain).isNotNull();
            assertThat(domain.uuid()).isNull();
            assertThat(savedDomain.uuid()).isNotNull();
            assertThat(savedDomain.identifier()).isEqualTo(domain.identifier());
            assertThat(savedDomain.description()).isEqualTo(domain.description());
            assertThat(savedDomain.source()).isEqualTo(domain.source());
            assertThat(savedDomain.enabled()).isEqualTo(domain.enabled());
            assertThat(savedDomain.createdAt()).isNotNull();
            assertThat(savedDomain.updatedAt()).isNotNull();
        }

        @Test
        void should_save_a_disabled_business_domain() {
            var domain = domainWithIdentifier("disabled_domain", false);

            var savedDomain = repository.save(domain);

            assertThat(savedDomain.enabled()).isFalse();
            assertThat(savedDomain.identifier().messageLaneIdentifier())
                .isEqualTo("disabled_domain");
        }

        @Test
        void should_save_several_business_domains() {
            repository.save(domainWithIdentifier("first_domain", true));
            repository.save(domainWithIdentifier("second_domain", true));

            assertThat(repository.findAll()).hasSize(2);
        }

        @Test
        void should_throw_when_the_business_domain_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null)
            );
        }
    }

    @Nested
    @DisplayName("find by identifier")
    class FindByIdentifier {
        @Test
        void should_find_the_business_domain() {
            var domain = BusinessDomainTestFixtures.createDefaultBusinessDomain();
            repository.save(domain);

            var foundDomain = repository.findByIdentifier(domain.identifier());

            assertThat(foundDomain).isNotNull();
            assertThat(foundDomain.identifier()).isEqualTo(domain.identifier());
            assertThat(foundDomain.description()).isEqualTo(domain.description());
            assertThat(foundDomain.source()).isEqualTo(domain.source());
            assertThat(foundDomain.enabled()).isEqualTo(domain.enabled());
            assertThat(foundDomain.createdAt()).isNotNull();
            assertThat(foundDomain.updatedAt()).isNotNull();
        }

        @Test
        void should_find_the_matching_business_domain_among_several() {
            var first = domainWithIdentifier("first_domain", true);
            var second = domainWithIdentifier("second_domain", false);
            repository.save(first);
            repository.save(second);

            var foundDomain = repository.findByIdentifier(second.identifier());

            assertThat(foundDomain).isNotNull();
            assertThat(foundDomain.identifier()).isEqualTo(second.identifier());
            assertThat(foundDomain.enabled()).isFalse();
        }

        @Test
        void should_return_null_when_the_identifier_is_unknown() {
            var domain = BusinessDomainTestFixtures.createDefaultBusinessDomain();

            var foundDomain = repository.findByIdentifier(domain.identifier());

            assertThat(foundDomain).isNull();
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
    @DisplayName("find all")
    class FindAll {
        @Test
        void should_return_all_the_business_domains() {
            var domain = BusinessDomainTestFixtures.createDefaultBusinessDomain();
            repository.save(domain);

            var domains = repository.findAll();

            assertThat(domains).hasSize(1);
        }

        @Test
        void should_return_an_empty_list_when_there_are_no_business_domains() {
            var domains = repository.findAll();

            assertThat(domains).isNotNull();
            assertThat(domains).isEmpty();
        }

        @Test
        void should_return_the_business_domains_sorted_by_identifier_ascending() {
            repository.save(domainWithIdentifier("b_domain", true));
            repository.save(domainWithIdentifier("c_domain", true));
            repository.save(domainWithIdentifier("a_domain", true));

            var identifiers = repository.findAll()
                                        .stream()
                                        .map(domain -> domain.identifier().messageLaneIdentifier())
                                        .toList();

            assertThat(identifiers).containsExactly("a_domain", "b_domain", "c_domain");
        }
    }

    @Nested
    @DisplayName("to domain")
    class ToDomain {
        @Test
        void should_return_null_when_the_entity_is_null() {
            assertThat(ConnectorDBBusinessDomainRepository.toDomain(null)).isNull();
        }

        @Test
        void should_map_all_the_fields_of_the_entity() {
            var source = BusinessDomainTestFixtures.createDefaultBusinessDomain().source();
            var entity = ConnectorBusinessDomainEntity
                .builder()
                .identifier("lane_identifier")
                .description("a description")
                .source(source)
                .enabled(true)
                .build();

            var domain = ConnectorDBBusinessDomainRepository.toDomain(entity);

            assertThat(domain).isNotNull();
            assertThat(domain.identifier().messageLaneIdentifier()).isEqualTo("lane_identifier");
            assertThat(domain.description()).isEqualTo("a description");
            assertThat(domain.source()).isEqualTo(source);
            assertThat(domain.enabled()).isTrue();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Nested
    @DisplayName("to entity")
    class ToEntity {
        @Test
        void should_map_all_the_fields_of_the_business_domain() {
            var domain = domainWithIdentifier("lane_identifier", false);

            var entity = ConnectorDBBusinessDomainRepository.toEntity(domain);

            assertThat(entity.getIdentifier()).isEqualTo("lane_identifier");
            assertThat(entity.getDescription()).isEqualTo(domain.description());
            assertThat(entity.getSource()).isEqualTo(domain.source());
            assertThat(entity.isEnabled()).isFalse();
        }

        @Test
        void should_throw_when_the_business_domain_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> ConnectorDBBusinessDomainRepository.toEntity(null)
            );
        }
    }
}
