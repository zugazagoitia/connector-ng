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
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import eu.ecodex.connector.infrastructure.adapter.out.auth.identity.ConnectorUserDetails;
import eu.ecodex.connector.infrastructure.config.property.auth.ConnectorJwtProperties;
import eu.ecodex.connector.infrastructure.config.property.auth.ConnectorRefreshTokenProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConnectorJwtParser}. This class contains tests for the JwtService class.
 */
class ConnectorJwtParserTest {
    private static final Instant FIXED_NOW = Instant.now();
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000L; // 15 min
    private static final String SECRET_STRING =
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd";
    private static final String ISSUER = "connector.ecodex.eu";

    private final ConnectorRefreshTokenProperties refreshTokenProps =
        new ConnectorRefreshTokenProperties(Duration.ofDays(3), "0 0 3 * * *");
    private final ConnectorUserDetails userDetails = ConnectorUserTestFixtures.createUserDetails();

    private final ConnectorJwtProperties jwtProperties =
        new ConnectorJwtProperties(
            SECRET_STRING, Duration.ofMillis(ACCESS_TOKEN_EXPIRATION_MS),
            ISSUER,
            refreshTokenProps
        );

    private ConnectorJwtGenerator jwtGenerator;
    private ConnectorJwtParser jwtParser;

    private ConnectorJwtParser jwtParseAt(ConnectorJwtProperties jwtProperties, Instant instant) {
        return new ConnectorJwtParser(jwtProperties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void initializeJwtParser(ConnectorJwtProperties jwtProperties) {
        jwtParser = jwtParseAt(jwtProperties, FIXED_NOW);
        jwtGenerator =
            new ConnectorJwtGenerator(jwtProperties, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void extractUsername_should_extract_username_from_token() {
        // Given
        initializeJwtParser(jwtProperties);
        var accessToken = jwtGenerator.generateAccessToken(userDetails);

        // When
        var extractedUsername = jwtParser.extractUsername(accessToken);

        // Then
        assertThat(extractedUsername).isEqualTo(userDetails.getUsername());
    }

    @Test
    void extractAuthorities() {
        // Given
        initializeJwtParser(jwtProperties);
        var accessToken = jwtGenerator.generateAccessToken(userDetails);

        // When
        var extractedAuthorities = jwtParser.extractAuthorities(accessToken);

        // Then
        assertThat(extractedAuthorities).isEqualTo(userDetails.getAuthorities());
    }

    @Test
    void isValidToken_should_return_FALSE_afterExpiration() {
        // Given
        initializeJwtParser(jwtProperties);
        var token = jwtGenerator.generateAccessToken(userDetails);

        var afterExpiry = FIXED_NOW.plusMillis(ACCESS_TOKEN_EXPIRATION_MS).plusSeconds(1);
        var jwtParseLater = jwtParseAt(jwtProperties, afterExpiry);

        assertThat(jwtParseLater.isValidToken(token, userDetails)).isFalse();
    }


    @Test
    void isValidToken_should_return_TRUE_when_token_is_valid() {
        // Given
        initializeJwtParser(jwtProperties);
        var token = jwtGenerator.generateAccessToken(userDetails);

        // When
        // Then
        assertThat(jwtParser.isValidToken(token, userDetails)).isTrue();
    }

    @Test
    void isValidToken_should_return_false_when_token_is_invalid() {
        // Given
        initializeJwtParser(jwtProperties);
        var token = jwtGenerator.generateAccessToken(userDetails);
        var otherUser = ConnectorUserDetails.builder().connectorUser(
            ConnectorUser.builder().build()
        ).build();

        // When
        // Then
        assertThat(jwtParser.isValidToken(token, otherUser)).isFalse();
    }

    @Test
    void isValidToken_should_return_false_when_token_is_tampered() {
        // Given
        initializeJwtParser(jwtProperties);
        var token = jwtGenerator.generateAccessToken(userDetails);
        var tampered = token.substring(0, token.length() - 2) + "xx";

        // When
        // Then
        assertThat(jwtParser.isValidToken(tampered, userDetails)).isFalse();
    }

    @Test
    void isValidToken_should_return_false_when_token_signed_ByDifferentKey() {
        // Given
        initializeJwtParser(jwtProperties);
        var otherKey = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba98765432";
        var props =
            new ConnectorJwtProperties(
                otherKey, Duration.ofMillis(ACCESS_TOKEN_EXPIRATION_MS),
                ISSUER,
                refreshTokenProps
            );

        var jwtServiceOtherKey =
            new ConnectorJwtGenerator(props, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        var tokenFromOtherIssuer = jwtServiceOtherKey.generateAccessToken(userDetails);

        // When
        // Then
        assertThat(jwtParser.isValidToken(tokenFromOtherIssuer, userDetails)).isFalse();
    }


    @Test
    void isExpired_should_return_TRUE_when_token_has_expired() {
        // Given
        initializeJwtParser(jwtProperties);
        var token = jwtGenerator.generateAccessToken(userDetails);

        var afterExpiry = FIXED_NOW.plusMillis(ACCESS_TOKEN_EXPIRATION_MS).plusSeconds(1);
        var expiredJwtParser = jwtParseAt(jwtProperties, afterExpiry);

        // When
        var isExpired = expiredJwtParser.isExpired(token);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    void isExpired_should_return_FALSE_when_token_has_not_expired() {
        // Given
        initializeJwtParser(jwtProperties);
        var token = jwtGenerator.generateAccessToken(userDetails);

        var afterExpiry = FIXED_NOW.plusMillis(ACCESS_TOKEN_EXPIRATION_MS).minusSeconds(1);
        var expiredJwtParser = jwtParseAt(jwtProperties, afterExpiry);

        // When
        var isExpired = expiredJwtParser.isExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    void parseAllowingExpired_should_parse_expired_token() {
        // Given
        initializeJwtParser(jwtProperties);
        var token = jwtGenerator.generateAccessToken(userDetails);

        var afterExpiry = FIXED_NOW.plusMillis(ACCESS_TOKEN_EXPIRATION_MS).plusSeconds(1);
        var expiredJwtParser = jwtParseAt(jwtProperties, afterExpiry);

        // When
        var claims = expiredJwtParser.parseAllowingExpired(token);

        // Then
        assertThat(claims).isNotEmpty();
        assertThat(claims.getSubject()).isEqualTo(userDetails.getUsername());
    }
}
