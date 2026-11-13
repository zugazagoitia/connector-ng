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

import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.BaseEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageAttachmentEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents the Connector Message Business Content entity used to manage and store information
 * about connector {@link ConnectorMessageBusinessContent} within the system.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "CONNECTOR_MESSAGE_BUSINESS_CONTENTS",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CM_BC_UUID", columnNames = "UUID"),
        @UniqueConstraint(name = "UQ_CM_BC_ATTACHMENT", columnNames = "XML_CONTENT_ATTACHMENT_ID"),
        @UniqueConstraint(name = "UQ_CM_BC_MESSAGE", columnNames = "MESSAGE_ID")
    }
)
public class ConnectorMessageBusinessContentEntity extends BaseEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "UUID", unique = true, nullable = false, updatable = false)
    private String uuid;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, optional = false)
    @JoinColumn(
        name = "XML_CONTENT_ATTACHMENT_ID",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_CM_BC_XML_CT_ATTACHMENT"))
    private ConnectorMessageAttachmentEntity xmlContent;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "MESSAGE_ID",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_CM_BC_MESSAGE"))
    private ConnectorMessageEntity message;

    @Setter
    @OneToOne(mappedBy = "businessContent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private ConnectorMessageBusinessDocumentEntity businessDocument;
}
