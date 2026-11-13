/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.transport;

import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the Connector Message Transport Step entity used to manage and store information about
 * the connector {@link ConnectorMessageTransportStep} within the system.
 */
@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "CONNECTOR_MESSAGE_TRANSPORT_STEPS",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CM_TS_IDENTIFIER", columnNames = "IDENTIFIER")
    },
    indexes = {
        @Index(name = "IDX_CM_TS_REMOTE_SYS_ID", columnList = "REMOTE_SYSTEM_IDENTIFIER"),
        @Index(name = "IDX_CM_TS_LINK_PARTNER", columnList = "LINK_PARTNER_NAME"),
        @Index(name = "IDX_CM_TS_STATUS", columnList = "STATUS")
    })
public class ConnectorMessageTransportStepEntity extends BaseEntity {
    private static final int TRANSPORTED_MESSAGE_SIZE = 5_000_000;

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "IDENTIFIER", nullable = false, updatable = false)
    private String identifier;

    @Column(name = "NUMBER_OF_ATTEMPTS", nullable = false)
    private int numberOfAttempts;

    @Column(name = "TRANSPORTED_MESSAGE_IDENTIFIER", nullable = false)
    private String transportedMessageIdentifier;

    @Column(name = "REMOTE_SYSTEM_IDENTIFIER")
    private String remoteSystemIdentifier;

    @Size(max = TRANSPORTED_MESSAGE_SIZE)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "TRANSPORTED_MESSAGE", length = TRANSPORTED_MESSAGE_SIZE, nullable = false)
    private String transportedMessage;

    @Column(name = "LINK_PARTNER_NAME", nullable = false, updatable = false)
    private String linkPartnerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private ConnectorMessageTransportStatus status;

    @Setter
    @Builder.Default
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "transportStep", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConnectorMessageTransportStepStatusEntity> statuses = new LinkedHashSet<>();
}
