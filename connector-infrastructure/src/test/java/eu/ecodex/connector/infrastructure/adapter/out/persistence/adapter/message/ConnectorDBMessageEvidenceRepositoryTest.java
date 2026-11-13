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

import eu.ecodex.connector.EvidenceTestFixtures;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageEvidenceRepository;
import eu.ecodex.connector.domain.model.message.evidence.ConnectorMessageEvidence;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageEvidenceEntity;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@SuppressWarnings("DataFlowIssue")
@DisplayName("ConnectorMessageEvidenceRepository")
public class ConnectorDBMessageEvidenceRepositoryTest extends AbstractRepositoryTest {
    private static final String MESSAGE_ID =
        "fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu";

    @Autowired
    private ConnectorMessageEvidenceRepository repository;

    /**
     * Reference data plus a seeded message, without evidences.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/message.sql",
    })
    private @interface WithMessageData {
    }

    @Nested
    @DisplayName("save")
    class Save {
        @Test
        @WithMessageData
        void should_save_the_evidence() {
            var evidence = EvidenceTestFixtures.createSubmissionAcceptanceEvidence();

            var saved = repository.save(evidence, MESSAGE_ID);

            assertThat(saved).isNotNull();
            assertThat(saved.uuid()).isNotNull();
            assertThat(saved.createdAt()).isNotNull();
            assertThat(saved.updatedAt()).isNotNull();
        }

        @Test
        @WithMessageData
        void should_persist_the_content_and_the_type() {
            var evidence = EvidenceTestFixtures.createSubmissionAcceptanceEvidence();

            var saved = repository.save(evidence, MESSAGE_ID);

            assertThat(saved.content()).isEqualTo(evidence.content());
            assertThat(saved.type()).isEqualTo(evidence.type());
        }

        @Test
        @WithMessageData
        void should_not_mark_the_evidence_as_delivered_when_it_is_saved() {
            var evidence = EvidenceTestFixtures.createSubmissionAcceptanceEvidence();

            var saved = repository.save(evidence, MESSAGE_ID);

            assertThat(saved.deliveredToLinkPartnerAt()).isNull();
        }

        @Test
        @WithMessageData
        void should_throw_when_the_evidence_content_is_null() {
            var evidenceWithoutContent = ConnectorMessageEvidence
                .builder()
                .type(EvidenceTestFixtures.createSubmissionAcceptanceEvidence().type())
                .content(null)
                .build();

            assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(evidenceWithoutContent, MESSAGE_ID)
            );
        }

        @Test
        void should_throw_when_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(
                    EvidenceTestFixtures.createSubmissionAcceptanceEvidence(),
                    null
                )
            );
        }

        @Test
        void should_throw_when_the_evidence_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null, MESSAGE_ID)
            );
        }

        @Test
        void should_throw_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null, null)
            );
        }
    }

    @Nested
    @DisplayName("find by uuid")
    class FindByUuid {
        @Test
        @WithMessageData
        void should_find_the_saved_evidence() {
            var evidence = EvidenceTestFixtures.createSubmissionAcceptanceEvidence();
            var saved = repository.save(evidence, MESSAGE_ID);

            var found = repository.findByUuid(saved.uuid());

            assertThat(found).isNotNull();
            assertThat(found.uuid()).isEqualTo(saved.uuid());
            assertThat(found.content()).isEqualTo(evidence.content());
            assertThat(found.type()).isEqualTo(evidence.type());
        }

        @Test
        void should_return_null_when_the_uuid_is_unknown() {
            var found = repository.findByUuid("unknown-uuid");

            assertThat(found).isNull();
        }
    }

    @Nested
    @DisplayName("set delivered to link partner at")
    class SetDeliveredToLinkPartnerAt {
        @Test
        @WithMessageData
        void should_set_the_delivery_date_of_the_evidence() {
            var saved = repository.save(
                EvidenceTestFixtures.createSubmissionAcceptanceEvidence(),
                MESSAGE_ID
            );
            assertThat(saved.deliveredToLinkPartnerAt()).isNull();

            repository.setDeliveredToLinkPartnerAt(saved.uuid());

            var found = repository.findByUuid(saved.uuid());
            assertThat(found.deliveredToLinkPartnerAt()).isNotNull();
        }

        @Test
        void should_throw_when_the_uuid_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.setDeliveredToLinkPartnerAt(null)
            );
        }
    }

    @Nested
    @DisplayName("to domain")
    class ToDomain {
        @Test
        void should_return_null_when_the_entity_is_null() {
            assertThat(ConnectorDBMessageEvidenceRepository.toDomain(null)).isNull();
        }

        @Test
        void should_map_all_the_fields_of_the_entity() {
            var type = EvidenceTestFixtures.createSubmissionAcceptanceEvidence().type();
            var entity = ConnectorMessageEvidenceEntity
                .builder()
                .content("<evidence/>")
                .type(type)
                .build();

            var evidence = ConnectorDBMessageEvidenceRepository.toDomain(entity);

            assertThat(evidence).isNotNull();
            assertThat(evidence.content())
                .isEqualTo("<evidence/>".getBytes(StandardCharsets.UTF_8));
            assertThat(evidence.type()).isEqualTo(type);
        }
    }
}
