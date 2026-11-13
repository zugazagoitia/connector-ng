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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ecodex.connector.ConnectorMessageDocumentTestFixtures;
import eu.ecodex.connector.MessageContentTestFixtures;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageBusinessContentRepository;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.AbstractRepositoryTest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@SuppressWarnings("DataFlowIssue")
@DisplayName("ConnectorMessageBusinessContentRepository")
public class ConnectorDBMessageBusinessContentRepositoryTest extends AbstractRepositoryTest {
    private static final String MESSAGE_ID =
        "fd2f35e0-1981-4d21-b718-10a802e884b0@connector.ecodex.eu";
    private static final String OTHER_MESSAGE_ID =
        "7b70aa96-dadc-4bca-87d8-5765846bf9ca@connector.ecodex.eu";

    @Autowired
    private ConnectorMessageBusinessContentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/message.sql",
        "classpath:sql/attachment.sql",
        "classpath:sql/message-business-content.sql",
        "classpath:sql/message-business-document.sql",
    })
    private @interface WithContentAndDocumentData {
    }

    private int contentsOf(String messageIdentifier) {
        return jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM connector_message_business_contents c
              JOIN connector_messages m ON c.message_id = m.id
             WHERE m.identifier = ?
            """,
            Integer.class,
            messageIdentifier
        );
    }

    private int documentsOf(String messageIdentifier) {
        return jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM connector_message_business_documents d
              JOIN connector_message_business_contents c ON d.business_content_id = c.id
              JOIN connector_messages m ON c.message_id = m.id
             WHERE m.identifier = ?
            """,
            Integer.class,
            messageIdentifier
        );
    }

    private int attachmentsOf(String messageIdentifier) {
        return jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM connector_message_attachments a
              JOIN connector_messages m ON a.message_id = m.id
             WHERE m.identifier = ?
            """,
            Integer.class,
            messageIdentifier
        );
    }

    @Nested
    @DisplayName("save")
    class Save {
        private static Stream<ConnectorMessageBusinessContent> createContents() {
            return Stream.of(
                MessageContentTestFixtures.createContent(),
                MessageContentTestFixtures.createContent()
                                          .toBuilder()
                                          .businessDocument(
                                              ConnectorMessageDocumentTestFixtures.createDocumentWithoutSignature())
                                          .build()
            );
        }

        @ParameterizedTest
        @MethodSource("createContents")
        @Sql({
            "classpath:sql/business-domain.sql",
            "classpath:sql/processing-mode.sql",
            "classpath:sql/message.sql",
            "classpath:sql/attachment2.sql",
        })
        void should_save_the_business_content(ConnectorMessageBusinessContent businessContent) {
            var savedBusinessContent = repository.save(businessContent, MESSAGE_ID);

            assertNotNull(savedBusinessContent);
            assertNotNull(savedBusinessContent.xmlContent());

            if (savedBusinessContent.businessDocument() != null) {
                assertNotNull(savedBusinessContent.businessDocument().uuid());
                assertNotNull(savedBusinessContent.businessDocument().content());
                assertThat(savedBusinessContent.businessDocument().aesType()).isEqualTo(
                    businessContent.businessDocument().aesType()
                );

                if (businessContent.businessDocument().detachedSignature() != null) {
                    assertThat(
                        savedBusinessContent.businessDocument().detachedSignature().identifier())
                        .isEqualTo(
                            businessContent.businessDocument().detachedSignature().identifier());
                }
            }
        }

        @Test
        void should_fail_when_the_content_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(null, MESSAGE_ID)
            );
        }

        @Test
        void should_fail_when_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.save(MessageContentTestFixtures.createContent(), null)
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
    @DisplayName("delete by message identifier")
    class DeleteByMessageIdentifier {
        @Test
        @WithContentAndDocumentData
        void should_delete_the_business_content_of_the_message() {
            assertThat(contentsOf(MESSAGE_ID)).isEqualTo(1);

            repository.deleteByMessageIdentifier(MESSAGE_ID);

            assertThat(contentsOf(MESSAGE_ID)).isZero();
        }

        @Test
        @WithContentAndDocumentData
        void should_delete_the_business_document_of_the_message() {
            assertThat(documentsOf(MESSAGE_ID)).isPositive();

            repository.deleteByMessageIdentifier(MESSAGE_ID);

            assertThat(documentsOf(MESSAGE_ID)).isZero();
        }

        @Test
        @WithContentAndDocumentData
        void should_not_delete_the_attachments() {
            var attachmentsBefore = attachmentsOf(MESSAGE_ID);

            repository.deleteByMessageIdentifier(MESSAGE_ID);

            // attachments are removed later, by the message deletion
            assertThat(attachmentsOf(MESSAGE_ID)).isEqualTo(attachmentsBefore);
        }

        @Test
        @WithContentAndDocumentData
        void should_not_delete_the_content_of_other_messages() {
            var otherContentsBefore = contentsOf(OTHER_MESSAGE_ID);
            var otherDocumentsBefore = documentsOf(OTHER_MESSAGE_ID);

            repository.deleteByMessageIdentifier(MESSAGE_ID);

            assertThat(contentsOf(OTHER_MESSAGE_ID)).isEqualTo(otherContentsBefore);
            assertThat(documentsOf(OTHER_MESSAGE_ID)).isEqualTo(otherDocumentsBefore);
        }

        @Test
        @WithContentAndDocumentData
        void should_be_idempotent_when_called_twice() {
            repository.deleteByMessageIdentifier(MESSAGE_ID);

            assertThatCode(() -> repository.deleteByMessageIdentifier(MESSAGE_ID))
                .doesNotThrowAnyException();
            assertThat(contentsOf(MESSAGE_ID)).isZero();
        }

        @Test
        @WithContentAndDocumentData
        void should_do_nothing_when_the_message_does_not_exist() {
            assertThatCode(() -> repository.deleteByMessageIdentifier("unknown"))
                .doesNotThrowAnyException();
        }

        @Test
        @Sql({
            "classpath:sql/business-domain.sql",
            "classpath:sql/processing-mode.sql",
            "classpath:sql/message.sql",
        })
        void should_do_nothing_when_the_message_has_no_business_content() {
            assertThatCode(() -> repository.deleteByMessageIdentifier(MESSAGE_ID))
                .doesNotThrowAnyException();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        void should_throw_when_the_message_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.deleteByMessageIdentifier(null)
            );
        }
    }
}
