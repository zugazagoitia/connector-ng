/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * BeanConfig class for configuring the infrastructure-specific components.
 */
@Configuration
@ConfigurationPropertiesScan
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class BeanConfig {
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .findAndRegisterModules()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Bean
    public S3Client s3Client(ConnectorFileStorageProperties properties) {
        var s3 = properties.s3();

        return S3Client.builder()
                       .region(Region.of(s3.region()))
                       .endpointOverride(s3.endpoint())
                       .credentialsProvider(
                           StaticCredentialsProvider.create(
                               AwsBasicCredentials.create(
                                   s3.accessKey(),
                                   s3.secretKey()
                               )
                           )
                       )
                       .forcePathStyle(s3.endpoint() != null)
                       .build();
    }
}
