/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.auth.accesstoken;

import static org.assertj.core.api.Assertions.assertThat;

import eu.ecodex.connector.ConnectorUserTestFixtures;
import eu.ecodex.connector.infrastructure.adapter.out.auth.identity.ConnectorUserDetails;
import eu.ecodex.connector.infrastructure.config.property.auth.ConnectorJwtProperties;
import eu.ecodex.connector.infrastructure.config.property.auth.ConnectorRefreshTokenProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConnectorJwtGenerator}.
 * This class contains tests for the JwtGenerator class.
 */
class ConnectorJwtGeneratorTest {
    private static final String CLEANUP_CRON = "0 0 3 * * *";
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(3);
    private static final Long ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000L; // 15 min
    private static final Instant FIXED_NOW = Instant.now();
    private static final String SECRET_STRING =
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd";
    private final ConnectorRefreshTokenProperties refreshTokenProps =
        new ConnectorRefreshTokenProperties(REFRESH_TOKEN_EXPIRATION, CLEANUP_CRON);
    private final ConnectorUserDetails userDetails = ConnectorUserTestFixtures.createUserDetails();

    private final ConnectorJwtProperties jwtProperties =
        new ConnectorJwtProperties(SECRET_STRING, Duration.ofMillis(ACCESS_TOKEN_EXPIRATION_MS),
            "", refreshTokenProps);

    @Test
    void generateToken_should_generate_valid_token() {
        // Given
        var jwtTokenGenerator =
            new ConnectorJwtGenerator(jwtProperties, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

        // When
        var accessToken = jwtTokenGenerator.generateAccessToken(userDetails);

        // Then
        assertThat(accessToken).isNotNull().isNotBlank();
        assertThat(accessToken.split("\\.")).hasSize(3); // header.payload.signature
    }
}
