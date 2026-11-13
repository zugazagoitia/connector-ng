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

import eu.ecodex.connector.BusinessDomainIdentifierTestFixtures;
import eu.ecodex.connector.PartyTestFixtures;
import eu.ecodex.connector.application.port.spi.pmode.ConnectorProcessingModePartyRepository;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomainIdentifier;
import eu.ecodex.connector.domain.model.pmode.ConnectorPartyRoleType;
import eu.ecodex.connector.domain.model.pmode.ConnectorProcessingModeParty;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.pmode.ConnectorProcessingModeEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.pmode.ConnectorProcessingModePartyEntity;
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

@DisplayName("ConnectorPartyRepository")
@SuppressWarnings({"checkstyle:MissingJavadocType", "checkstyle:LineLength", "DataFlowIssue"})
public class ConnectorDBProcessingModePartyRepositoryTest extends AbstractRepositoryTest {
    private static final ConnectorBusinessDomainIdentifier DEFAULT_BUSINESS_DOMAIN =
        BusinessDomainIdentifierTestFixtures.createDefaultBusinessDomainIdentifier();
    private static final String ECODEX_IDENTIFIER_TYPE =
        "urn:oasis:names:tc:ebcore:partyid-type:ecodex";
    @Autowired
    private ConnectorProcessingModePartyRepository repository;

    /**
     * Reference data (business domain + processing mode), without parties.
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
     * Reference data plus seeded parties.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
    })
    private @interface WithPartyData {
    }

    @Nested
    @DisplayName("save all")
    class SaveAll {
        @Test
        @WithProcessingModeData
        void should_save_all_the_parties() {
            var party = PartyTestFixtures.createToParty();

            var savedParties = repository.saveAll(List.of(party), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedParties).isNotNull();
            assertThat(savedParties).hasSize(1);
        }

        @Test
        @WithProcessingModeData
        void should_persist_all_the_party_fields() {
            var party = PartyTestFixtures.createToParty();

            var savedParties = repository.saveAll(List.of(party), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedParties)
                .usingRecursiveComparison()
                .isEqualTo(List.of(party));
        }

        @Test
        @WithProcessingModeData
        void should_return_an_empty_list_when_there_are_no_parties_to_save() {
            var savedParties = repository.saveAll(List.of(), DEFAULT_BUSINESS_DOMAIN);

            assertThat(savedParties).isEmpty();
        }

        @Test
        void should_fail_when_the_parties_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.saveAll(null, DEFAULT_BUSINESS_DOMAIN)
            );
        }

        @Test
        void should_fail_when_the_business_domain_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.saveAll(List.of(PartyTestFixtures.createToParty()), null)
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
    @DisplayName("find by identifier, role and business domain")
    class FindByIdentifierRoleAndBusinessDomain {
        @Test
        @WithPartyData
        void should_find_the_party() {
            var found = repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                "BL",
                ConnectorPartyRoleType.INITIATOR,
                DEFAULT_BUSINESS_DOMAIN
            );

            assertThat(found).isNotNull();
        }

        @Test
        @WithPartyData
        void should_map_all_the_fields_of_the_found_party() {
            var found = repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                "RE",
                ConnectorPartyRoleType.INITIATOR,
                DEFAULT_BUSINESS_DOMAIN
            );

            assertThat(found).isNotNull();
            assertThat(found.name()).isEqualTo("service_red_ecodex");
            assertThat(found.identifier()).isEqualTo("RE");
            assertThat(found.identifierType()).isEqualTo(ECODEX_IDENTIFIER_TYPE);
            assertThat(found.role()).isEqualTo("GW");
            assertThat(found.roleType()).isEqualTo(ConnectorPartyRoleType.INITIATOR);
            assertThat(found.isHome()).isFalse();
        }

        @Test
        @WithPartyData
        void should_distinguish_the_parties_by_role_type() {
            var initiator = repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                "BL",
                ConnectorPartyRoleType.INITIATOR,
                DEFAULT_BUSINESS_DOMAIN
            );
            var responder = repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                "BL",
                ConnectorPartyRoleType.RESPONDER,
                DEFAULT_BUSINESS_DOMAIN
            );

            assertThat(initiator).isNotNull();
            assertThat(responder).isNotNull();
            assertThat(initiator.roleType()).isEqualTo(ConnectorPartyRoleType.INITIATOR);
            assertThat(responder.roleType()).isEqualTo(ConnectorPartyRoleType.RESPONDER);
        }

        @Test
        @WithPartyData
        void should_return_null_when_the_party_identifier_is_unknown() {
            var found = repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                "AT",
                ConnectorPartyRoleType.INITIATOR,
                DEFAULT_BUSINESS_DOMAIN
            );

            assertThat(found).isNull();
        }

        @Test
        @WithProcessingModeData
        void should_return_null_when_there_are_no_parties() {
            var found = repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                "BL",
                ConnectorPartyRoleType.INITIATOR,
                DEFAULT_BUSINESS_DOMAIN
            );

            assertThat(found).isNull();
        }

        @Test
        void should_fail_when_the_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                    null,
                    ConnectorPartyRoleType.INITIATOR,
                    DEFAULT_BUSINESS_DOMAIN
                )
            );
        }

        @Test
        void should_fail_when_the_role_type_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                    "AT",
                    null,
                    DEFAULT_BUSINESS_DOMAIN
                )
            );
        }

        @Test
        void should_fail_when_the_business_domain_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByIdentifierAndRoleTypeAndBusinessDomain(
                    "AT",
                    ConnectorPartyRoleType.INITIATOR,
                    null
                )
            );
        }

        @Test
        void should_fail_when_all_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByIdentifierAndRoleTypeAndBusinessDomain(null, null, null)
            );
        }
    }

    @Nested
    @DisplayName("find all by business domain identifier")
    class FindAllByBusinessDomainIdentifier {
        @Test
        @WithPartyData
        void should_find_all_the_parties() {
            var parties = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN);

            assertThat(parties).isNotNull();
            assertThat(parties).hasSize(4);
        }

        @Test
        @WithProcessingModeData
        void should_return_an_empty_list_when_there_are_no_parties() {
            var parties = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN);

            assertThat(parties).isEmpty();
        }

        @Test
        @WithPartyData
        void should_return_the_parties_sorted_by_identifier_ascending() {
            var identifiers = repository.findAllByBusinessDomainIdentifier(DEFAULT_BUSINESS_DOMAIN)
                                        .stream()
                                        .map(ConnectorProcessingModeParty::identifier)
                                        .toList();

            assertThat(identifiers).containsExactly("BL", "BL", "RE", "RE");
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
            assertThat(ConnectorDBProcessingModePartyRepository.toDomain(null)).isNull();
        }

        @Test
        void should_map_all_the_fields_of_the_entity() {
            var entity = ConnectorProcessingModePartyEntity
                .builder()
                .name("service_blue_ecodex")
                .identifier("BL")
                .identifierType(ECODEX_IDENTIFIER_TYPE)
                .role("GW")
                .roleType(ConnectorPartyRoleType.RESPONDER)
                .isHome(true)
                .build();

            var party = ConnectorDBProcessingModePartyRepository.toDomain(entity);

            assertThat(party).isNotNull();
            assertThat(party.name()).isEqualTo("service_blue_ecodex");
            assertThat(party.identifier()).isEqualTo("BL");
            assertThat(party.identifierType()).isEqualTo(ECODEX_IDENTIFIER_TYPE);
            assertThat(party.role()).isEqualTo("GW");
            assertThat(party.roleType()).isEqualTo(ConnectorPartyRoleType.RESPONDER);
            assertThat(party.isHome()).isTrue();
        }
    }

    @Nested
    @DisplayName("to entity")
    class ToEntity {
        @Test
        void should_map_all_the_fields_and_the_processing_mode() {
            var processingMode = mock(ConnectorProcessingModeEntity.class);
            var party = PartyTestFixtures.createToParty();

            var entity = ConnectorDBProcessingModePartyRepository.toEntity(party, processingMode);

            assertThat(entity.getName()).isEqualTo(party.name());
            assertThat(entity.getIdentifier()).isEqualTo(party.identifier());
            assertThat(entity.getIdentifierType()).isEqualTo(party.identifierType());
            assertThat(entity.getRole()).isEqualTo(party.role());
            assertThat(entity.getRoleType()).isEqualTo(party.roleType());
            assertThat(entity.isHome()).isEqualTo(party.isHome());
            assertThat(entity.getProcessingMode()).isSameAs(processingMode);
        }
    }
}
