/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.content;

import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.content.ConnectorMessageBusinessDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for performing CRUD operations on the
 * {@link ConnectorMessageBusinessDocumentEntity}.
 */
@Repository
public interface ConnectorMessageBusinessDocumentJpaRepository extends
    JpaRepository<ConnectorMessageBusinessDocumentEntity, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    DELETE FROM ConnectorMessageBusinessDocumentEntity d
     WHERE d.businessContent.id IN (
        SELECT c.id FROM ConnectorMessageBusinessContentEntity c
         WHERE c.message.id = (
            SELECT m.id FROM ConnectorMessageEntity m WHERE m.identifier = :identifier))
    """)
    int deleteByMessageIdentifier(@Param("identifier") String identifier);
}
