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

import eu.ecodex.connector.application.port.spi.ConnectorFileStorageProvider;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 Implementation of the {@link ConnectorFileStorageProvider}.
 */
@Slf4j
@Component
public class ConnectorS3FileStorageProvider implements ConnectorFileStorageProvider {
    private final S3Client s3Client;
    private final ConnectorFileStorageProperties storageProperties;

    public ConnectorS3FileStorageProvider(
        S3Client s3Client,
        ConnectorFileStorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    @Override
    public String save(@NonNull ConnectorMessageAttachment attachment, @NonNull Path filePath) {
        log.debug("Saving attachment [{}] to s3", attachment.identifier());

        var putObjectRequest = buildObjectRequest(attachment);

        this.s3Client.putObject(
            putObjectRequest,
            RequestBody.fromFile(filePath)
        );

        return attachment.identifier();
    }

    @Override
    public String save(@NonNull ConnectorMessageAttachment attachment, byte @NonNull [] content) {
        var putObjectRequest = buildObjectRequest(attachment);

        this.s3Client.putObject(
            putObjectRequest,
            RequestBody.fromBytes(content)
        );

        return attachment.identifier();
    }

    @Override
    public byte[] findByIdentifier(@NonNull String identifier) {
        log.debug("Downloading attachment [{}] from s3", identifier);

        var getObjectRequest = GetObjectRequest.builder()
                                               .bucket(this.storageProperties.s3().bucket())
                                               .key(identifier)
                                               .build();

        try {
            var responseBytes = this.s3Client.getObjectAsBytes(getObjectRequest);
            return responseBytes.asByteArray();
        } catch (Exception e) {
            log.error("Could not download attachment [{}]", identifier, e);
            return null;
        }
    }

    @Override
    public ConnectorAttachmentStorage getStorage() {
        return ConnectorAttachmentStorage.S3_BUCKET;
    }

    @Override
    public void deleteByIdentifier(@NonNull String identifier) {
        log.debug("Deleting attachment [{}] from s3", identifier);

        var deleteObjectRequest = DeleteObjectRequest.builder()
                                                     .bucket(this.storageProperties.s3().bucket())
                                                     .key(identifier)
                                                     .build();

        this.s3Client.deleteObject(deleteObjectRequest);
    }

    private PutObjectRequest buildObjectRequest(ConnectorMessageAttachment attachment) {
        return PutObjectRequest.builder()
                               .bucket(this.storageProperties.s3().bucket())
                               .key(attachment.identifier())
                               .build();
    }
}
