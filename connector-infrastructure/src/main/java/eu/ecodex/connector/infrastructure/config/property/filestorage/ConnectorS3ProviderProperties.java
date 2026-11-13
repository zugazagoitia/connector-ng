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

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.Builder;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties required to connect to an Amazon S3 (or compatible) object storage
 * service.
 */
@Builder
public record ConnectorS3ProviderProperties(
    @DefaultValue("true")
    boolean autoCreateBucket,

    @NotBlank
    String region,

    @NotBlank
    String accessKey,

    @NotBlank
    String secretKey,

    @NotBlank
    @Pattern(
        regexp = "[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]",
        message = "must be a valid S3 bucket name"
    )
    String bucket,

    URI endpoint
) {
    /**
     * Validates that the endpoint, when present, is an absolute {@code http} or {@code https} URL
     * with a host.
     *
     * @return {@code true} if the endpoint is {@code null} or an absolute http(s) URL with a host,
     *     {@code false} otherwise
     */
    @AssertTrue(message = "endpoint must be an absolute http(s) URL, e.g. http://localhost:9000")
    public boolean isEndpointValid() {
        return endpoint == null
            || (endpoint.getScheme() != null
            && (endpoint.getScheme().equalsIgnoreCase("http")
            || endpoint.getScheme().equalsIgnoreCase("https"))
            && endpoint.getHost() != null);
    }
}
