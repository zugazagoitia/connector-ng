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

import eu.ecodex.connector.application.port.spi.message.ConnectorMessageBusinessContentRepository;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessContent;
import eu.ecodex.connector.domain.model.message.content.ConnectorMessageBusinessDocument;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageAttachmentEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.ConnectorMessageEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.content.ConnectorMessageBusinessContentEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.message.content.ConnectorMessageBusinessDocumentEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.ConnectorMessageAttachmentJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.ConnectorMessageJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.content.ConnectorMessageBusinessContentJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.message.content.ConnectorMessageBusinessDocumentJpaRepository;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RDBMS implementation of {@link ConnectorMessageBusinessContentRepository}.
 */
@Component
@Transactional(readOnly = true)
@SuppressWarnings("checkstyle:LineLength")
public class ConnectorDBMessageBusinessContentRepository implements
    ConnectorMessageBusinessContentRepository {
    private final ConnectorMessageBusinessContentJpaRepository businessContentJpaRepository;
    private final ConnectorMessageJpaRepository messageJpaRepository;
    private final ConnectorMessageBusinessDocumentJpaRepository businessDocumentJpaRepository;
    private final ConnectorMessageAttachmentJpaRepository attachmentJpaRepository;

    /**
     * Constructs a new instance of ConnectorMessageBusinessContentRepositoryImpl.
     *
     * @param businessContentJpaRepository  the repository for managing
     *                                      {@link ConnectorMessageBusinessContentEntity} entities
     * @param messageJpaRepository          the repository for managing
     *                                      {@link ConnectorMessageEntity} entities
     * @param businessDocumentJpaRepository the repository for managing
     *                                      {@link ConnectorMessageBusinessDocumentEntity} entities
     * @param attachmentJpaRepository       the repository for managing
     *                                      {@link ConnectorMessageAttachmentEntity} entities
     */
    public ConnectorDBMessageBusinessContentRepository(
        ConnectorMessageBusinessContentJpaRepository businessContentJpaRepository,
        ConnectorMessageJpaRepository messageJpaRepository,
        ConnectorMessageBusinessDocumentJpaRepository businessDocumentJpaRepository,
        ConnectorMessageAttachmentJpaRepository attachmentJpaRepository) {
        this.businessContentJpaRepository = businessContentJpaRepository;
        this.messageJpaRepository = messageJpaRepository;
        this.businessDocumentJpaRepository = businessDocumentJpaRepository;
        this.attachmentJpaRepository = attachmentJpaRepository;
    }

    static ConnectorMessageBusinessContent toDomain(
        ConnectorMessageBusinessContentEntity entity) {
        if (entity == null) {
            return null;
        }

        return ConnectorMessageBusinessContent
            .builder()
            .uuid(entity.getUuid())
            .xmlContent(ConnectorDBMessageAttachmentRepository.toDomain(entity.getXmlContent()))
            .businessDocument(toDomain(entity.getBusinessDocument()))
            .build();
    }

    private static ConnectorMessageBusinessDocument toDomain(
        ConnectorMessageBusinessDocumentEntity entity) {
        if (entity == null) {
            return null;
        }

        return ConnectorMessageBusinessDocument
            .builder()
            .uuid(entity.getUuid())
            .aesType(entity.getAesType())
            .content(ConnectorDBMessageAttachmentRepository.toDomain(entity.getContent()))
            .detachedSignature(ConnectorDBMessageAttachmentRepository.toDomain(entity.getDetachedSignature()))
            .build();
    }

    @Override
    @Transactional
    public ConnectorMessageBusinessContent save(
        @NonNull ConnectorMessageBusinessContent businessContent,
        @NonNull String messageIdentifier) {
        var message = this.messageJpaRepository.findByIdentifier(messageIdentifier);
        var contentToSave = toEntity(businessContent, message);
        var savedContent = this.businessContentJpaRepository.save(contentToSave);

        if (businessContent.businessDocument() != null) {
            var businessDocumentToSave = toEntity(businessContent.businessDocument(), savedContent);
            var savedBusinessDocument = this.businessDocumentJpaRepository.save(
                businessDocumentToSave);
            savedContent.setBusinessDocument(savedBusinessDocument);
        }

        return toDomain(savedContent);
    }

    @Override
    @Transactional
    public ConnectorMessageBusinessContent assignBusinessDocument(
        @NonNull String uuid,
        @NonNull ConnectorMessageBusinessDocument document) {
        var content = this.businessContentJpaRepository.findByUuid(uuid);
        var businessDocumentToSave = toEntity(document, content);

        var savedBusinessDocument = this.businessDocumentJpaRepository.save(businessDocumentToSave);

        content.setBusinessDocument(savedBusinessDocument);

        return toDomain(content);
    }

    @Override
    @Transactional
    public void deleteByMessageIdentifier(@NonNull String messageIdentifier) {
        this.businessDocumentJpaRepository.deleteByMessageIdentifier(messageIdentifier);
        this.businessContentJpaRepository.deleteByMessageIdentifier(messageIdentifier);
    }

    private ConnectorMessageBusinessContentEntity toEntity(
        ConnectorMessageBusinessContent content, ConnectorMessageEntity message) {
        var xmlContent = this.attachmentJpaRepository.findByIdentifier(
            content.xmlContent().identifier());

        return ConnectorMessageBusinessContentEntity
            .builder()
            .xmlContent(xmlContent)
            .message(message)
            .build();
    }

    private ConnectorMessageBusinessDocumentEntity toEntity(
        ConnectorMessageBusinessDocument businessDocument,
        ConnectorMessageBusinessContentEntity businessContent) {
        var documentContent = this.attachmentJpaRepository.findByIdentifier(
            businessDocument.content().identifier());

        var detachedSignature = businessDocument.detachedSignature();
        ConnectorMessageAttachmentEntity savedDetachedSignature = null;
        if (detachedSignature != null) {
            savedDetachedSignature = this.attachmentJpaRepository.findByIdentifier(
                detachedSignature.identifier()
            );
        }

        return ConnectorMessageBusinessDocumentEntity
            .builder()
            .aesType(businessDocument.aesType())
            .businessContent(businessContent)
            .content(documentContent)
            .detachedSignature(savedDetachedSignature)
            .build();
    }
}
