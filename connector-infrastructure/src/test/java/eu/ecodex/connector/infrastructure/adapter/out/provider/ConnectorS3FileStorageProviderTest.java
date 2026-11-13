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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorS3ProviderProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorS3FileStorageProvider")
public class ConnectorS3FileStorageProviderTest {
    @TempDir
    Path tempDir;

    @Mock
    private S3Client s3Client;
    @Mock
    private ConnectorFileStorageProperties fileStorageProperties;

    @InjectMocks
    private ConnectorS3FileStorageProvider fileStorageProvider;

    @BeforeEach
    void setUp() {
        lenient().when(fileStorageProperties.s3()).thenReturn(
            ConnectorS3ProviderProperties.builder()
                                         .bucket("attachments")
                                         .autoCreateBucket(false)
                                         .secretKey("test-secret")
                                         .accessKey("test-access")
                                         .endpoint(URI.create("http://fake-endpoint"))
                                         .build()
        );
    }

    private Path provideTemporaryPath() throws IOException {
        return Files.createTempDirectory(tempDir, "test-upload-");
    }

    @Nested
    @DisplayName("save a file")
    class SaveFile {
        @Test
        void should_store_the_file() throws IOException {
            when(fileStorageProperties.s3()).thenReturn(
                ConnectorS3ProviderProperties.builder()
                                             .bucket("test-bucket")
                                             .autoCreateBucket(false)
                                             .secretKey("test-secret")
                                             .accessKey("test-access")
                                             .endpoint(URI.create("http://fake-endpoint"))
                                             .build()
            );
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

            var attachment = MessageAttachmentTestFixtures.createAttachment();
            var identifier = fileStorageProvider.save(attachment, provideTemporaryPath());

            assertThat(identifier).endsWith("test_attachment");
        }

        @Test
        void should_throw_when_the_path_is_null() {
            var attachment = MessageAttachmentTestFixtures.createAttachment();

            assertThrows(
                NullPointerException.class,
                () -> fileStorageProvider.save(attachment, (Path) null)
            );
        }

        @Test
        void should_throw_when_the_attachment_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> fileStorageProvider.save(null, provideTemporaryPath())
            );
        }

        @Test
        void should_throw_when_both_arguments_are_null() {
            assertThrows(
                NullPointerException.class,
                () -> fileStorageProvider.save(null, (Path) null)
            );
        }
    }

    @Nested
    @DisplayName("find by identifier")
    class FindByIdentifier {
        @Test
        void should_find_the_file() {
            var attachment = MessageAttachmentTestFixtures.createAttachment();
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(
                    GetObjectResponse.builder().build(), new byte[1]));

            var foundAttachment = fileStorageProvider.findByIdentifier(attachment.identifier());

            assertThat(foundAttachment).isNotNull();
            assertThat(foundAttachment).hasSize(1);
        }

        @Test
        void should_return_null_when_the_identifier_is_unknown() {
            var attachment = MessageAttachmentTestFixtures.createAttachment();
            doThrow(RuntimeException.class).when(s3Client)
                                           .getObjectAsBytes(any(GetObjectRequest.class));
            var foundAttachment = fileStorageProvider.findByIdentifier(attachment.identifier());

            assertThat(foundAttachment).isNull();
        }

        @Test
        void should_failed_when_the_identifier_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> fileStorageProvider.findByIdentifier(null)
            );
        }
    }
}
