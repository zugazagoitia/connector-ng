/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.filestorage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;


/**
 * Configuration properties of the connector file storage, bound from the
 * {@code connector.file.storage.*} prefix.
 */
@Validated
@ConfigurationProperties(prefix = "connector.file.storage")
public record ConnectorFileStorageProperties(
    @NotNull
    Path uploadTempDirectory,

    @Valid
    @NotNull
    @NestedConfigurationProperty
    ConnectorS3ProviderProperties s3
) {
    @AssertTrue(message = "upload-temp-directory must be a writable directory")
    public boolean isUploadTempDirectoryUsable() {
        return uploadTempDirectory == null
            || (Files.isDirectory(uploadTempDirectory) && Files.isWritable(uploadTempDirectory));
    }
}
