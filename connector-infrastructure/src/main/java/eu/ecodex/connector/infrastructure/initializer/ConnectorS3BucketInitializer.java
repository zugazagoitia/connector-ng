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

import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Creates the S3 bucket used by the connector at application startup, if it does not exist yet.
 *
 * <p>The initializer is active only when the property {@code connector.s3.auto-create-bucket} is
 * set to {@code true}; otherwise the bucket must already exist. It runs as an
 * {@link ApplicationRunner}, after the application context has been loaded. The bucket name comes
 * from {@link ConnectorFileStorageProperties#s3()}.
 *
 * <p>The existence of the bucket is checked with a {@code HEAD} request. If the bucket is not
 * found (HTTP 404), it is created. If several instances start at the same time and another one
 * creates the bucket first, the resulting {@link BucketAlreadyOwnedByYouException} is ignored. Any
 * other S3 error fails the startup.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "connector.s3.auto-create-bucket",
    havingValue = "true",
    matchIfMissing = true
)
public class ConnectorS3BucketInitializer implements ApplicationRunner {
    private final S3Client s3Client;
    private final ConnectorFileStorageProperties properties;

    /**
     * Creates the initializer.
     *
     * @param s3Client   the client used to check for and create the bucket (must not be null)
     * @param properties the S3 provider properties, providing the bucket name (must not be null)
     */
    public ConnectorS3BucketInitializer(
        S3Client s3Client,
        ConnectorFileStorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    /**
     * Ensures that the configured bucket exists, creating it if needed.
     *
     * @param args the application arguments (not used)
     *
     * @throws S3Exception if the bucket check fails with a status other than 404 (for example 403
     *                     when access is denied), or if the creation fails for a reason other than
     *                     the bucket already being owned by this account
     */
    @Override
    public void run(ApplicationArguments args) {
        var bucket = properties.s3().bucket();

        try {
            s3Client.headBucket(b -> b.bucket(bucket));
            log.debug("S3 bucket [{}] already exists", bucket);
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw e;
            }
            log.info("Creating S3 bucket [{}]", bucket);
            try {
                s3Client.createBucket(b -> b.bucket(bucket));
            } catch (BucketAlreadyOwnedByYouException ignored) {
                // created concurrently by another instance
                log.info("S3 bucket [{}] already exists", bucket);
            }
        }
    }
}
