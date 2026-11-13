/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.initializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorS3ProviderProperties;
import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorS3BucketInitializer")
class ConnectorS3BucketInitializerTest {
    private static final String BUCKET = "attachments";

    @Mock
    private S3Client s3Client;
    @Mock
    private ConnectorFileStorageProperties properties;

    @Captor
    private ArgumentCaptor<Consumer<HeadBucketRequest.Builder>> headBucketCaptor;
    @Captor
    private ArgumentCaptor<Consumer<CreateBucketRequest.Builder>> createBucketCaptor;

    private ConnectorS3BucketInitializer initializer;

    @BeforeEach
    void setUp() {
        lenient().when(properties.s3()).thenReturn(
            ConnectorS3ProviderProperties.builder()
                                         .bucket(BUCKET)
                                         .autoCreateBucket(false)
                                         .secretKey("test-secret")
                                         .accessKey("test-access")
                                         .endpoint(URI.create("http://fake-endpoint"))
                                         .build());

        initializer = new ConnectorS3BucketInitializer(s3Client, properties);
    }

    private S3Exception s3Exception(int statusCode) {
        return (S3Exception) S3Exception.builder().statusCode(statusCode).build();
    }

    private Consumer<HeadBucketRequest.Builder> anyHeadBucketConsumer() {
        return ArgumentMatchers.any();
    }

    private Consumer<CreateBucketRequest.Builder> anyCreateBucketConsumer() {
        return ArgumentMatchers.any();
    }

    @Nested
    @DisplayName("when the bucket already exists")
    class BucketExists {
        @Test
        void should_check_the_configured_bucket() {
            initializer.run(mock(ApplicationArguments.class));

            verify(s3Client).headBucket(headBucketCaptor.capture());
            var builder = HeadBucketRequest.builder();
            headBucketCaptor.getValue().accept(builder);
            assertThat(builder.build().bucket()).isEqualTo(BUCKET);
        }

        @Test
        void should_not_create_the_bucket() {
            initializer.run(mock(ApplicationArguments.class));

            verify(s3Client, never()).createBucket(anyCreateBucketConsumer());
        }
    }

    @Nested
    @DisplayName("when the bucket is not found")
    class BucketNotFound {
        @BeforeEach
        void bucketIsNotFound() {
            when(s3Client.headBucket(anyHeadBucketConsumer())).thenThrow(s3Exception(404));
        }

        @Test
        void should_create_the_bucket() {
            initializer.run(mock(ApplicationArguments.class));

            verify(s3Client).createBucket(anyCreateBucketConsumer());
        }

        @Test
        void should_create_the_configured_bucket() {
            initializer.run(mock(ApplicationArguments.class));

            verify(s3Client).createBucket(createBucketCaptor.capture());
            var builder = CreateBucketRequest.builder();
            createBucketCaptor.getValue().accept(builder);
            assertThat(builder.build().bucket()).isEqualTo(BUCKET);
        }

        @Test
        void should_ignore_the_conflict_when_the_bucket_was_created_concurrently() {
            when(s3Client.createBucket(anyCreateBucketConsumer()))
                .thenThrow(BucketAlreadyOwnedByYouException.builder().build());

            assertThatCode(() -> initializer.run(mock(ApplicationArguments.class)))
                .doesNotThrowAnyException();
        }

        @Test
        void should_rethrow_when_the_creation_fails_for_another_reason() {
            var exception = s3Exception(500);
            when(s3Client.createBucket(anyCreateBucketConsumer())).thenThrow(exception);

            assertThatThrownBy(() -> initializer.run(mock(ApplicationArguments.class)))
                .isSameAs(exception);
        }

        @Test
        void should_fail_when_the_bucket_name_is_taken_by_another_account() {
            when(s3Client.createBucket(anyCreateBucketConsumer()))
                .thenThrow(BucketAlreadyExistsException.builder().build());

            assertThatThrownBy(() -> initializer.run(mock(ApplicationArguments.class)))
                .isInstanceOf(BucketAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("when the bucket check fails with a status other than 404")
    class BucketCheckFails {
        @ParameterizedTest
        @ValueSource(ints = {301, 400, 403, 500})
        void should_rethrow_the_exception(int status) {
            var exception = s3Exception(status);
            when(s3Client.headBucket(anyHeadBucketConsumer())).thenThrow(exception);

            assertThatThrownBy(() -> initializer.run(mock(ApplicationArguments.class)))
                .isSameAs(exception);
        }

        @ParameterizedTest
        @ValueSource(ints = {301, 400, 403, 500})
        void should_not_create_the_bucket(int status) {
            when(s3Client.headBucket(anyHeadBucketConsumer())).thenThrow(s3Exception(status));

            assertThatThrownBy(() -> initializer.run(mock(ApplicationArguments.class)))
                .isInstanceOf(S3Exception.class);

            verify(s3Client, never()).createBucket(anyCreateBucketConsumer());
        }
    }
}
