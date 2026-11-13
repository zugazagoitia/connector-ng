/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.transport;

import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.transport.ConnectorMessageTransportStepEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for performing CRUD operations on the
 * {@link ConnectorMessageTransportStepEntity}.
 */
@Repository
public interface ConnectorMessageTransportStepJpaRepository extends
    JpaRepository<ConnectorMessageTransportStepEntity, Long>,
    JpaSpecificationExecutor<ConnectorMessageTransportStepEntity> {
    @EntityGraph(attributePaths = {"statuses"})
    ConnectorMessageTransportStepEntity findByIdentifier(String identifier);

    @Query("""
        SELECT mts
        FROM ConnectorMessageTransportStepEntity mts
        WHERE mts.identifier = :identifier
            OR mts.transportedMessageIdentifier = :identifier
            OR mts.remoteSystemIdentifier = :identifier
        """
    )
    ConnectorMessageTransportStepEntity findByTransportOrMessageOrRemoteSystemIdentifier(
        @Param("identifier") String identifier);

    @Query("""
        SELECT DISTINCT mts.identifier
        FROM ConnectorMessageTransportStepEntity mts
        WHERE mts.status = READY_FOR_PULL
            AND mts.linkPartnerName = :backendName
        """
    )
    List<String> findAllPendingByBackendName(@Param("backendName") String backendName);

    @Modifying
    @Transactional
    @Query("""
        UPDATE ConnectorMessageTransportStepEntity mts
            SET mts.status = :status,
               mts.numberOfAttempts = mts.numberOfAttempts + 1,
               mts.updatedAt = :now
        WHERE mts.identifier IN :identifiers
        """)
    void updateStatus(
        @Param("identifiers") List<String> identifiers,
        @Param("status") ConnectorMessageTransportStatus status,
        @Param("now") Instant now);

    List<ConnectorMessageTransportStepEntity> findAllByStatusAndLinkPartnerNameOrderByCreatedAtAsc(
        ConnectorMessageTransportStatus status, String linkPartnerName
    );

    void deleteByTransportedMessageIdentifier(String messageIdentifier);
}
