/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ecodex.connector.application.port.spi.message.ConnectorMessageTransportStepRepository;
import eu.ecodex.connector.domain.model.filter.ConnectorTransportStepsListFilter;
import eu.ecodex.connector.domain.model.message.ConnectorBusinessMessage;
import eu.ecodex.connector.domain.model.message.ConnectorEvidenceMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStep;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStepStatus;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.PaginationMapper;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.transport.ConnectorMessageTransportStepEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.transport.ConnectorMessageTransportStepStatusEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.transport.ConnectorMessageTransportStepJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.transport.ConnectorMessageTransportStepStatusJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.transport.specification.TransportStepSpecification;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RDBMS implementation of {@link ConnectorMessageTransportStepRepository}.
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class ConnectorDBMessageTransportStepRepository
    implements ConnectorMessageTransportStepRepository {
    private final ConnectorMessageTransportStepJpaRepository transportStepJpaRepository;
    private final ConnectorMessageTransportStepStatusJpaRepository stepStatusJpaRepository;
    private final PaginationMapper paginationMapper;
    private final ObjectMapper objectMapper;

    /**
     * Constructs an instance of {@code ConnectorMessageTransportStepRepositoryImpl}.
     *
     * @param transportStepJpaRepository the repository for managing
     *                                   {@code ConnectorMessageTransportStepEntity} instances
     * @param stepStatusJpaRepository    the repository for managing
     *                                   {@code ConnectorMessageTransportStepStatusEntity}
     *                                   instances
     * @param paginationMapper           the utility for mapping between page requests and results
     * @param objectMapper               The object mapper
     */
    public ConnectorDBMessageTransportStepRepository(
        ConnectorMessageTransportStepJpaRepository transportStepJpaRepository,
        ConnectorMessageTransportStepStatusJpaRepository stepStatusJpaRepository,
        PaginationMapper paginationMapper,
        ObjectMapper objectMapper) {
        this.transportStepJpaRepository = transportStepJpaRepository;
        this.stepStatusJpaRepository = stepStatusJpaRepository;
        this.paginationMapper = paginationMapper;

        this.objectMapper = objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @Transactional
    public ConnectorMessageTransportStep save(
        @NonNull ConnectorMessageTransportStep transportStep) {
        try {
            var transportStepEntity = toEntity(transportStep);

            var stepStatusEntity = toEntity(transportStep.status());
            var savedTransportStep = transportStepJpaRepository.save(transportStepEntity);
            stepStatusEntity.setTransportStep(savedTransportStep);
            var savedStepStatus = stepStatusJpaRepository.save(stepStatusEntity);

            savedTransportStep.getStatuses().add(savedStepStatus);

            return toDomain(savedTransportStep);
        } catch (JsonProcessingException e) {
            log.error("Could not parse transported message", e);
            throw new IllegalArgumentException("Could not parse transported message", e);
        }
    }

    @Override
    @Transactional
    public ConnectorMessageTransportStep update(
        @NonNull String identifier,
        @NonNull ConnectorMessageTransportStatus status,
        int numberOfAttempts) {
        var entity = transportStepJpaRepository.findByIdentifier(identifier);

        if (entity == null) {
            throw new IllegalArgumentException(
                "No transport step found for identifier: " + identifier
            );
        }

        var updatedEntity = entity.toBuilder()
                                  .numberOfAttempts(numberOfAttempts)
                                  .status(status)
                                  .build();
        var updated = this.transportStepJpaRepository.save(updatedEntity);

        var stepStatusEntity = toEntity(status);
        stepStatusEntity.setTransportStep(updated);
        var savedStepStatusEntity = this.stepStatusJpaRepository.save(stepStatusEntity);

        updated.getStatuses().add(savedStepStatusEntity);

        return toDomain(updated);
    }

    @Override
    @Transactional
    public void updateStatus(
        @NonNull List<String> identifiers,
        @NonNull ConnectorMessageTransportStatus status) {
        var now = Instant.now();
        this.transportStepJpaRepository.updateStatus(identifiers, status, now);

        this.stepStatusJpaRepository.insert(identifiers, status.name(), now);
    }

    @Override
    public ConnectorMessageTransportStep findByStepOrMessageOrRemoteSystemIdentifier(
        @NonNull String identifier) {
        var entity = this.transportStepJpaRepository
            .findByTransportOrMessageOrRemoteSystemIdentifier(identifier);

        return toDomain(entity);
    }

    @Override
    public ConnectorMessageTransportStep findByIdentifier(@NonNull String identifier) {
        var entity = this.transportStepJpaRepository.findByIdentifier(identifier);

        return toDomain(entity);
    }

    @Override
    public List<String> findReadyToDownloadTransportStepsIds(@NonNull String backendName) {
        return transportStepJpaRepository.findAllPendingByBackendName(backendName);
    }

    @Override
    public List<ConnectorMessageTransportStep> findReadyToDownloadTransportSteps(
        @NonNull String backendName) {
        return transportStepJpaRepository.findAllByStatusAndLinkPartnerNameOrderByCreatedAtAsc(
            ConnectorMessageTransportStatus.READY_FOR_PULL,
            backendName
        ).stream().map(this::toDomain).toList();
    }

    @Override
    public ConnectorPageResult<ConnectorMessageTransportStep> findAll(
        @NonNull ConnectorTransportStepsListFilter filter) {
        var pageable = paginationMapper.toPageable(filter.pageRequest());
        var specification = TransportStepSpecification.withFilters(filter);
        var transportSteps = transportStepJpaRepository.findAll(specification, pageable)
                                                       .map(this::toDomain);

        return paginationMapper.toPageResult(transportSteps);
    }

    @Override
    @Transactional
    public void deleteByTransportedMessageIdentifier(@NonNull String messageIdentifier) {
        transportStepJpaRepository.deleteByTransportedMessageIdentifier(messageIdentifier);
    }

    private ConnectorMessageTransportStep toDomain(ConnectorMessageTransportStepEntity entity) {
        if (entity == null) {
            return null;
        }

        try {
            var transportedMessage = deserializeMessage(entity.getTransportedMessage());

            return ConnectorMessageTransportStep
                .builder()
                .identifier(entity.getIdentifier())
                .remoteSystemIdentifier(entity.getRemoteSystemIdentifier())
                .transportedMessageIdentifier(entity.getTransportedMessageIdentifier())
                .numberOfAttempts(entity.getNumberOfAttempts())
                .linkPartnerName(entity.getLinkPartnerName())
                .transportedMessage(transportedMessage)
                .status(entity.getStatus())
                .statuses(toStatuses(entity.getStatuses()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        } catch (JsonProcessingException e) {
            log.error("Could not parse transported message", e);
            return null;
        }
    }

    private ConnectorMessageTransportStepEntity toEntity(
        ConnectorMessageTransportStep transportStep) throws JsonProcessingException {
        var transportedMessage = transportStep.transportedMessage();
        return ConnectorMessageTransportStepEntity
            .builder()
            .identifier(transportStep.identifier())
            .numberOfAttempts(transportStep.numberOfAttempts())
            .linkPartnerName(transportStep.linkPartnerName())
            .transportedMessageIdentifier(transportStep.transportedMessageIdentifier())
            .remoteSystemIdentifier(transportStep.remoteSystemIdentifier())
            .transportedMessage(
                objectMapper.writeValueAsString(transportStep.transportedMessage()))
            .status(transportStep.status())
            .build();
    }

    private ConnectorMessageTransportStepStatusEntity toEntity(
        ConnectorMessageTransportStatus status) {
        return ConnectorMessageTransportStepStatusEntity.builder().status(status).build();
    }

    private Set<ConnectorMessageTransportStepStatus> toStatuses(
        Set<ConnectorMessageTransportStepStatusEntity> statuses) {
        return statuses.stream()
                       .map(status -> ConnectorMessageTransportStepStatus
                           .builder()
                           .status(status.getStatus())
                           .createdAt(status.getCreatedAt())
                           .build())
                       .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ConnectorMessage deserializeMessage(String json)
        throws JsonProcessingException {

        var node = objectMapper.readTree(json);

        if (node.hasNonNull("businessContent")) {
            return objectMapper.treeToValue(node, ConnectorBusinessMessage.class);
        }

        if (node.hasNonNull("transportedEvidences")) {
            return objectMapper.treeToValue(node, ConnectorEvidenceMessage.class);
        }

        throw new IllegalStateException(
            "Cannot classify transported message: neither businessContent nor "
                + "transportedEvidences present"
        );
    }
}
