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

import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessageStatus;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.BaseEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.ConnectorBusinessDomainEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.content.ConnectorMessageBusinessContentEntity;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the Connector Message entity used to manage and store information about connector
 * {@link ConnectorBusinessMessage} within the system.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CONNECTOR_MESSAGES",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CM_IDENTIFIER", columnNames = "IDENTIFIER")
    },
    indexes = {
        @Index(name = "IDX_CM_BACKEND_NAME", columnList = "BACKEND_NAME"),
        @Index(name = "IDX_CM_BACKEND_MSG_ID", columnList = "BACKEND_MESSAGE_IDENTIFIER"),
        @Index(name = "IDX_CM_GATEWAY_NAME", columnList = "GATEWAY_NAME"),
        @Index(name = "IDX_CM_DIRECTION", columnList = "DIRECTION"),
        @Index(name = "IDX_CM_DELIVERED_AT", columnList = "DELIVERED_TO_LINK_PARTNER_AT"),
        @Index(name = "IDX_CM_CREATED_AT", columnList = "CREATED_AT"),
        @Index(name = "IDX_CM_BUSINESS_DOMAIN_ID", columnList = "BUSINESS_DOMAIN_ID")
    })
public class ConnectorMessageEntity extends BaseEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "IDENTIFIER", nullable = false)
    private String identifier;

    @Setter
    @Column(name = "BACKEND_MESSAGE_IDENTIFIER")
    private String backendMessageIdentifier;

    @Setter
    @Column(name = "BACKEND_NAME")
    private String backendName;

    @Setter
    @Column(name = "GATEWAY_NAME")
    private String gatewayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIRECTION", nullable = false)
    private ConnectorMessageDirection direction;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private ConnectorBusinessMessageStatus status;

    @Setter
    @Column(name = "DELETED_AT")
    private Instant deletedAt;

    @Setter
    @Column(name = "REJECTED_AT")
    private Instant rejectedAt;

    @Setter
    @Column(name = "CONFIRMED_AT")
    private Instant confirmedAt;

    @Setter
    @Column(name = "DELIVERED_TO_LINK_PARTNER_AT")
    private Instant deliveredToLinkPartnerAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "BUSINESS_DOMAIN_ID",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_CM_BUSINESS_DOMAIN")
    )
    private ConnectorBusinessDomainEntity businessDomain;

    @Setter
    @OneToOne(mappedBy = "message", orphanRemoval = true)
    private ConnectorMessageBusinessContentEntity businessContent;

    @Setter
    @OneToOne(mappedBy = "message", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private ConnectorMessageAS4PropertiesEntity as4Properties;

    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConnectorMessageAttachmentEntity> attachments = new ArrayList<>();

    @Builder.Default
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConnectorMessageEvidenceEntity> evidences = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConnectorMessageErrorEntity> errors = new ArrayList<>();
}
