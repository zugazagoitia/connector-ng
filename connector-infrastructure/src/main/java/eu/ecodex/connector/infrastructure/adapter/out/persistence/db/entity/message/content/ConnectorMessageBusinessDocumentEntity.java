/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.content;

import eu.ecodex.connector.domain.model.message.content.ConnectorBusinessDocumentAESType;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessDocument;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.BaseEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageAttachmentEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents the Connector Message Business Content entity used to manage and store information
 * about connector {@link ConnectorMessageBusinessDocument} within the system.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "CONNECTOR_MESSAGE_BUSINESS_DOCUMENTS",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CM_BD_UUID", columnNames = "UUID"),
        @UniqueConstraint(name = "UQ_CM_BD_CONTENT", columnNames = "BUSINESS_CONTENT_ID"),
        @UniqueConstraint(name = "UQ_CM_BD_ATTACHMENT", columnNames = "CONTENT_ATTACHMENT_ID"),
        @UniqueConstraint(
            name = "UQ_CM_BD_DS_ATTACHMENT", columnNames = "DETACHED_SIGNATURE_ATTACHMENT_ID"
        )
    },
    indexes = {
        @Index(name = "IDX_CM_BD_AES_TYPE", columnList = "AES_TYPE")
    }
)
public class ConnectorMessageBusinessDocumentEntity extends BaseEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "UUID", nullable = false, updatable = false)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "AES_TYPE", updatable = false)
    private ConnectorBusinessDocumentAESType aesType;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "BUSINESS_CONTENT_ID",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_CM_BD_CONTENT")
    )
    private ConnectorMessageBusinessContentEntity businessContent;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, optional = false)
    @JoinColumn(
        name = "CONTENT_ATTACHMENT_ID",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_CM_BD_ATT_CONTENT")
    )
    private ConnectorMessageAttachmentEntity content;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(
        name = "DETACHED_SIGNATURE_ATTACHMENT_ID",
        foreignKey = @ForeignKey(name = "FK_CM_BD_DS_ATTACHMENT")
    )
    private ConnectorMessageAttachmentEntity detachedSignature;
}
