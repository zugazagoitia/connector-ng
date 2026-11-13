/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector;

import eu.ecodex.connector.domain.model.user.ConnectorUser;
import eu.ecodex.connector.infrastructure.adapter.in.messaging.inbound.ConnectorJmsGatewayMessageAcknowledgementListener;
import eu.ecodex.connector.infrastructure.adapter.in.messaging.inbound.ConnectorJmsGatewayMessageListener;
import eu.ecodex.connector.infrastructure.adapter.in.messaging.inbound.ConnectorJmsInboundMessagePipelineListener;
import eu.ecodex.connector.infrastructure.adapter.in.messaging.outbound.ConnectorJmsBackendMessageDeliveryListener;
import eu.ecodex.connector.infrastructure.adapter.in.messaging.outbound.ConnectorJmsOutboundMessagePipelineListener;
import eu.ecodex.connector.infrastructure.adapter.in.messaging.outbound.ConnectorJmsOutboundMessageStagingListener;
import eu.ecodex.connector.infrastructure.adapter.out.auth.accesstoken.ConnectorJwtGenerator;
import eu.ecodex.connector.infrastructure.adapter.out.auth.identity.ConnectorUserDetails;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@WithUploadTempDirectory
@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {
    private static final DockerImageName MINIO_IMAGE =
        DockerImageName.parse("pgsty/minio:RELEASE.2026-08-04T00-00-00Z")
            .asCompatibleSubstituteFor("minio/minio");
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.33");

    public static final MinIOContainer minio;
    public static final MySQLContainer mysql;
    private static final MinioClient minioClient;

    static {
        minio = new MinIOContainer(MINIO_IMAGE)
            .withUserName("testuser")
            .withPassword("testpassword")
            .withStartupTimeout(Duration.ofMinutes(2));

        mysql = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("connector")
            .withUsername("connector")
            .withPassword("connector");

        Startables.deepStart(minio, mysql).join();

        try {
            minioClient = MinioClient.builder()
                .endpoint(minio.getS3URL())
                .credentials(minio.getUserName(), minio.getPassword())
                .build();
            createBucketIfNotExists();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }

    @MockitoBean
    ConnectorJmsBackendMessageDeliveryListener backendMessageDeliveryListener;
    @MockitoBean
    ConnectorJmsGatewayMessageAcknowledgementListener gatewayMessageAcknowledgementListener;
    @MockitoBean
    ConnectorJmsGatewayMessageListener gatewayMessageListener;
    @MockitoBean
    ConnectorJmsInboundMessagePipelineListener inboundMessagePipelineListener;
    @MockitoBean
    ConnectorJmsOutboundMessagePipelineListener outboundMessagePipelineListener;
    @MockitoBean
    ConnectorJmsOutboundMessageStagingListener outboundMessageStagingListener;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected ConnectorJwtGenerator jwtTokenService;

    @DynamicPropertySource
    static void registerPropertiesMain(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver"
        );
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.defer-datasource-initialization", () -> "true");

        registry.add("connector.file.storage.s3.access-key", minio::getUserName);
        registry.add("connector.file.storage.s3.secret-key", minio::getPassword);
        registry.add("connector.file.storage.s3.bucket", () -> "attachments");
        registry.add("connector.file.storage.s3.region", () -> "us-east-1");
        registry.add("connector.file.storage.s3.endpoint", minio::getS3URL);
    }

    private static void createBucketIfNotExists() throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket("attachments").build()
        );
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket("attachments").build()
            );
        }
    }

    protected MultiValueMap<String, Object> produceAttachmentPart(int fileSize) {
        var parts = new LinkedMultiValueMap<String, Object>();

        parts.add(
            "attachments",
            FilePartTestFixtures.filePart(
                "fake_file.pdf",
                FileTestFixtures.generateFakeFile(fileSize),
                MediaType.APPLICATION_PDF
            )
        );

        return parts;
    }

    protected void cleanDb() {
        jdbcTemplate.execute((Connection con) -> {
            try (Statement st = con.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : List.of(
                    "connector_message_transport_step_statuses",
                    "connector_message_transport_steps",
                    "connector_message_evidences",
                    "connector_message_errors",
                    "connector_message_business_documents",
                    "connector_message_business_contents",
                    "connector_message_as4_properties",
                    "connector_message_attachments",
                    "connector_messages",
                    "connector_processing_mode_parties",
                    "connector_processing_mode_services",
                    "connector_processing_mode_actions",
                    "connector_processing_modes",
                    "connector_processing_mode_truststores",
                    "connector_business_domains",
                    "connector_refresh_tokens",
                    "connector_users",
                    "connector_roles",
                    "connector_users_roles"
                )) {
                    st.execute("TRUNCATE TABLE " + table);
                }
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });
    }

    protected String generateDefaultAdminToken() {
        var user = new ConnectorUserDetails(ConnectorUser.defaultAdminUser().toBuilder()
            .uuid(UUID.randomUUID().toString())
            .build());
        return jwtTokenService.generateAccessToken(user);
    }
}
