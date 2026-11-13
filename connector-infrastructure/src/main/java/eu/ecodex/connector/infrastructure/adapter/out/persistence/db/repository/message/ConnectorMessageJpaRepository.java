/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message;

import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for performing CRUD operations on the {@link ConnectorMessageEntity}.
 */
@Repository
public interface ConnectorMessageJpaRepository extends
    JpaRepository<ConnectorMessageEntity, Long>,
    JpaSpecificationExecutor<ConnectorMessageEntity> {
    ConnectorMessageEntity findByIdentifier(String identifier);

    List<ConnectorMessageEntity> findByAs4PropertiesConversationIdentifier(
        String conversationIdentifier);

    ConnectorMessageEntity findByAs4PropertiesEbmsMessageIdentifierAndDirection(
        String ebmsMessageIdentifier, ConnectorMessageDirection direction);

    ConnectorMessageEntity findByBackendMessageIdentifier(String backendMessageIdentifier);

    List<ConnectorMessageEntity> findByIdentifierIn(List<String> identifiers);

    @Query("""
        SELECT m
        FROM ConnectorMessageEntity m
        WHERE m.confirmedAt IS NULL
            AND m.rejectedAt IS NULL
            AND m.createdAt <= :threshold
        """
    )
    List<ConnectorMessageEntity> findUnresolveToDelete(@Param("threshold") Instant threshold);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE ConnectorMessageEntity m
        SET m.deletedAt = :deletedAt,
           m.updatedAt = :deletedAt,
           m.status = DELETED
        WHERE m.identifier = :identifier
            AND m.deletedAt IS NULL
        """
    )
    void markAsDeleted(@Param("identifier") String identifier,
                      @Param("deletedAt") Instant deletedAt);

    void deleteByIdentifier(String identifier);
}
