/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.auth;

import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.user.ConnectorRefreshTokenEntity;
import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for managing and accessing {@link ConnectorRefreshTokenEntity} entities
 * in the database.
 * This interface extends {@link JpaRepository}, providing methods for CRUD operations and custom
 * queries.
 *
 * <p>The {@link ConnectorRefreshTokenEntity} represents refresh tokens used for authentication
 * and authorization in the system.
 *
 *
 * <p>Dependencies:
 * - Utilizes {@link ConnectorRefreshTokenEntity}, which contains details like the token,
 * expiration, and revocation status.
 * - Works within the persistence context provided by Spring Data JPA.
 */
@Repository
public interface ConnectorUserRefreshTokenJpaRepository
    extends JpaRepository<ConnectorRefreshTokenEntity, Long> {
    /**
     * Retrieves a {@link ConnectorRefreshTokenEntity} instance matching the specified token.
     * This method is used to look up refresh token entities in the database by their unique token
     * value.
     *
     * @param tokenHashed the unique token string used to identify the
     *                    {@link ConnectorRefreshTokenEntity}
     *
     * @return an {@link Optional} containing the {@link ConnectorRefreshTokenEntity} if found, or
     *     an empty {@link Optional} if no entity matches the provided token
     */
    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<ConnectorRefreshTokenEntity> findByToken(@Nonnull String tokenHashed);

    /**
     * Retrieves a list of {@link ConnectorRefreshTokenEntity} instances based on the specified
     * user's unique identifier
     * (UUID) and revocation status.
     *
     * @param userUuid the unique identifier (UUID) of the user whose refresh token entities are to
     *                 be retrieved
     * @param revoked  the revocation status used to filter the returned refresh token entities
     *
     * @return a list of {@link ConnectorRefreshTokenEntity} instances matching the provided user
     *     UUID and revocation status;
     *     if no entities match, an empty list is returned
     */
    @EntityGraph(attributePaths = {"user", "user.roles"})
    List<ConnectorRefreshTokenEntity> findByUser_UuidAndRevoked(@Nonnull String userUuid,
                                                                boolean revoked);

    @Modifying
    @Transactional
    @Query("""
        UPDATE ConnectorRefreshTokenEntity rt
           SET rt.revoked = true, rt.revokedAt= :now
         WHERE rt.user.uuid = :userUuid
           AND rt.revoked = false
        """
    )
    @EntityGraph(attributePaths = {"user", "user.roles"})
    int revokeAllActiveByUserId(@Param("userUuid") @Nonnull String userUuid,
                                @Param("now") Instant now);

    @Modifying
    @Transactional
    @EntityGraph(attributePaths = {"user", "user.roles"})
    int deleteByUser_Uuid(@Nonnull String userUuid);

    @Modifying
    @Transactional
    @EntityGraph(attributePaths = {"user", "user.roles"})
    int deleteByExpiresAtBefore(@Nonnull Instant instant);

    @Modifying
    @Transactional
    @EntityGraph(attributePaths = {"user", "user.roles"})
    int deleteByRevokedTrueAndExpiresAtBefore(@Nonnull Instant instant);
}
