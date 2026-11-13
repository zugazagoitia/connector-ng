/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.provider;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ecodex.connector.RepositoryContextConfiguration;
import eu.ecodex.connector.WithUploadTempDirectory;
import eu.ecodex.connector.application.port.spi.link.ConnectorLinkPartnerProvider;
import eu.ecodex.connector.domain.model.link.partner.ConnectorLinkPartnerName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@WithUploadTempDirectory
@SuppressWarnings("DataFlowIssue")
@DisplayName("ConnectorLinkPartnerRepository")
@ActiveProfiles(value = {"dss", "security", "evidence", "local"})
@SpringBootTest(
    classes = RepositoryContextConfiguration.class
)
public class ConnectorLinkPartnerProviderTest {
    @Autowired
    private ConnectorLinkPartnerProvider repository;

    @Nested
    @DisplayName("find all")
    class FindAll {
        @Test
        void should_load_all_the_link_partners_from_properties() {
            var partners = repository.findAll();

            assertThat(partners).isNotNull();
            assertThat(partners).hasSize(3);
        }
    }

    @Nested
    @DisplayName("find by name")
    class FindByName {
        @Test
        void should_find_the_link_partner() {
            var name = ConnectorLinkPartnerName.builder().name("backend_alice").build();

            var partner = repository.findByName(name);

            assertThat(partner).isNotNull();
            assertThat(partner.name()).isEqualTo(name);
        }

        @Test
        void should_throw_when_the_name_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByName(null)
            );
        }
    }

    @Nested
    @DisplayName("find by certificate DN")
    class FindByCertificateDn {
        @Test
        void should_find_the_link_partner() {
            var partner = repository.findByCertificateDn("cn=alice");

            assertThat(partner).isNotNull();
            assertThat(partner.name().name()).isEqualTo("backend_alice");
        }

        @Test
        void should_throw_when_the_certificate_dn_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByCertificateDn(null)
            );
        }
    }
}
