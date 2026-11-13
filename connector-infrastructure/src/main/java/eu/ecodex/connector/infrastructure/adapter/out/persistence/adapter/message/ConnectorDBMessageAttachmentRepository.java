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

import eu.ecodex.connector.application.port.spi.message.ConnectorMessageAttachmentRepository;
import eu.ecodex.connector.domain.model.filter.ConnectorAttachmentsListFilter;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.PaginationMapper;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageAttachmentEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.ConnectorMessageAttachmentJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.ConnectorMessageJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.specification.AttachmentSpecification;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RDBMS implementation of {@link ConnectorMessageAttachmentRepository}.
 */
@Component
@Transactional(readOnly = true)
public class ConnectorDBMessageAttachmentRepository implements
    ConnectorMessageAttachmentRepository {
    private static final int BATCH_SIZE = 500;

    private final ConnectorMessageAttachmentJpaRepository attachmentJpaRepository;
    private final ConnectorMessageJpaRepository messageJpaRepository;
    private final PaginationMapper paginationMapper;

    /**
     * Constructs an instance of {@code ConnectorMessageAttachmentRepositoryImpl} with the specified
     * dependencies.
     *
     * @param attachmentJpaRepository the repository for performing CRUD operations on
     *                                {@code ConnectorMessageAttachmentEntity}
     * @param messageJpaRepository    the repository for performing CRUD operations on
     *                                {@code ConnectorMessageEntity}
     * @param paginationMapper        the utility for mapping between page requests and results
     */
    public ConnectorDBMessageAttachmentRepository(
        ConnectorMessageAttachmentJpaRepository attachmentJpaRepository,
        ConnectorMessageJpaRepository messageJpaRepository, PaginationMapper paginationMapper) {
        this.attachmentJpaRepository = attachmentJpaRepository;
        this.messageJpaRepository = messageJpaRepository;
        this.paginationMapper = paginationMapper;
    }

    static ConnectorMessageAttachment toDomain(ConnectorMessageAttachmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return ConnectorMessageAttachment
            .builder()
            .identifier(entity.getIdentifier())
            .name(entity.getName())
            .size(entity.getSize())
            .contentType(entity.getContentType())
            .description(entity.getDescription())
            .storage(entity.getStorage())
            .type(entity.getType())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .deletedAt(entity.getDeletedAt())
            .build();
    }

    @Override
    @Transactional
    public ConnectorMessageAttachment save(ConnectorMessageAttachment attachment) {
        var attachmentToSave = toEntity(attachment);
        var savedAttachment = attachmentJpaRepository.save(attachmentToSave);

        return toDomain(savedAttachment);
    }

    @Override
    public ConnectorMessageAttachment findByIdentifier(@NonNull String identifier) {
        var attachment = this.attachmentJpaRepository.findByIdentifier(identifier);

        return toDomain(attachment);
    }

    @Override
    public boolean existsByIdentifier(@NonNull String identifier) {
        return this.attachmentJpaRepository.existsByIdentifier(identifier);
    }

    @Override
    public List<ConnectorMessageAttachment> findAllActiveByMessageIdentifier(
        @NonNull String messageIdentifier) {
        var attachments = this.attachmentJpaRepository.findAllByMessageIdentifierAndDeletedAtIsNull(
            messageIdentifier);

        return attachments.stream().map(ConnectorDBMessageAttachmentRepository::toDomain).toList();
    }

    @Override
    public List<ConnectorMessageAttachment> findByMessageIdentifierAndTypes(
        @NonNull String messageIdentifier,
        @NonNull List<ConnectorAttachmentType> types) {
        var specification = AttachmentSpecification.hasMessageIdentifierAndTypeIn(
            messageIdentifier,
            types
        );

        return attachmentJpaRepository.findAll(specification)
            .stream()
            .map(ConnectorDBMessageAttachmentRepository::toDomain)
            .toList();
    }

    @Override
    public ConnectorPageResult<ConnectorMessageAttachment> findAll(
        @NonNull ConnectorAttachmentsListFilter filter) {
        var pageable = paginationMapper.toPageable(filter.pageRequest());

        var specification = AttachmentSpecification.withFilters(filter);

        var attachments = attachmentJpaRepository
            .findAll(specification, pageable)
            .map(ConnectorDBMessageAttachmentRepository::toDomain);

        return paginationMapper.toPageResult(attachments);
    }

    @Override
    public List<ConnectorMessageAttachment> findAllOrphaned(Instant threshold) {
        return attachmentJpaRepository.findOrphansToDelete(threshold)
            .stream()
            .map(ConnectorDBMessageAttachmentRepository::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void attachToMessage(
        @NonNull String attachmentIdentifier,
        @NonNull String messageIdentifier) {
        var attachment = attachmentJpaRepository.findByIdentifier(attachmentIdentifier);

        if (attachment.getMessage() != null) {
            throw new IllegalStateException("attachment already assigned to a message");
        }

        var message = messageJpaRepository.findByIdentifier(messageIdentifier);
        attachment.setMessage(message);
        attachmentJpaRepository.save(attachment);
    }

    @Override
    @Transactional
    public void updateType(
        @NonNull String attachmentIdentifier, @NonNull ConnectorAttachmentType type) {
        var attachment = this.attachmentJpaRepository.findByIdentifier(attachmentIdentifier);
        attachment.setType(type);
        this.attachmentJpaRepository.save(attachment);
    }

    @Override
    @Transactional
    public void markAsDeleted(List<String> identifiers, Instant deletedAt) {
        if (identifiers.isEmpty()) {
            return;
        }

        var list = List.copyOf(identifiers);
        for (int from = 0; from < list.size(); from += BATCH_SIZE) {
            var batch = list.subList(from, Math.min(from + BATCH_SIZE, list.size()));
            attachmentJpaRepository.markAsDeleted(batch, deletedAt);
        }
    }

    @Override
    @Transactional
    public void deleteAllByIdentifiers(@NonNull List<String> identifiers) {
        if (identifiers.isEmpty()) {
            return;
        }

        var list = List.copyOf(identifiers);
        for (int from = 0; from < list.size(); from += BATCH_SIZE) {
            var batch = list.subList(from, Math.min(from + BATCH_SIZE, list.size()));
            attachmentJpaRepository.deleteAllByIdentifiers(batch);
        }
    }

    private ConnectorMessageAttachmentEntity toEntity(ConnectorMessageAttachment attachment) {
        return ConnectorMessageAttachmentEntity
            .builder()
            .identifier(attachment.identifier())
            .name(attachment.name())
            .size(attachment.size())
            .contentType(attachment.contentType())
            .description(attachment.description())
            .storage(attachment.storage())
            .type(attachment.type())
            .build();
    }
}
