/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message;

import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.BaseEntity;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the Connector Message Attachment entity used to manage and store information about
 * connector {@link ConnectorMessageAttachment} within the system.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "CONNECTOR_MESSAGE_ATTACHMENTS",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CM_ATTACHMENTS_IDENTIFIER", columnNames = "IDENTIFIER")
    },
    indexes = {
        @Index(name = "IDX_CM_ATTACHMENTS_NAME", columnList = "NAME"),
        @Index(name = "IDX_CM_ATTACHMENTS_TYPE", columnList = "TYPE"),
        @Index(name = "IDX_CM_ATTACHMENTS_MESSAGE_ID", columnList = "MESSAGE_ID")
    }
)
public class ConnectorMessageAttachmentEntity extends BaseEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "IDENTIFIER", nullable = false, updatable = false)
    private String identifier;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CONTENT_TYPE", nullable = false, updatable = false)
    private String contentType;

    @Column(name = "FILE_SIZE", nullable = false)
    private long size;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "STORAGE", nullable = false, updatable = false)
    private ConnectorAttachmentStorage storage;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE")
    private ConnectorAttachmentType type;

    @Column(name = "DELETED_AT")
    private Instant deletedAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "MESSAGE_ID",
        foreignKey = @ForeignKey(name = "FK_CM_ATTACHMENTS_MESSAGE"))
    private ConnectorMessageEntity message;
}
