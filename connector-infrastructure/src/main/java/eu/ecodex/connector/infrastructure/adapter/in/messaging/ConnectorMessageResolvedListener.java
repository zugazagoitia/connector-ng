/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.messaging;

import eu.ecodex.connector.application.port.api.message.ConnectorCleanupResolvedMessageAttachments;
import eu.ecodex.connector.domain.model.message.ConnectorMessageResolvedEvent;
import eu.ecodex.connector.infrastructure.adapter.in.ConnectorEventHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JMS listener that cleans up the attachments of a message once it has been resolved.
 *
 * <p>It consumes {@link ConnectorMessageResolvedEvent}s from the queue configured under
 * {@code connector.queues.resolved-message-cleanup-processing-queue} and delegates to
 * {@link ConnectorCleanupResolvedMessageAttachments}, which removes the attachments of the resolved
 * message from the file storage and marks them as deleted.
 */

@Slf4j
@Component
@Transactional
public class ConnectorMessageResolvedListener
    implements ConnectorEventHandler<ConnectorMessageResolvedEvent> {
    private final ConnectorCleanupResolvedMessageAttachments cleanup;

    /**
     * Creates the listener.
     *
     * @param cleanup the use case that cleans up the attachments of a resolved message (must not be
     *                null)
     */

    public ConnectorMessageResolvedListener(ConnectorCleanupResolvedMessageAttachments cleanup) {
        this.cleanup = cleanup;
    }

    @Override
    @JmsListener(destination = "connector.queues.resolved-message-cleanup-processing-queue")
    public void handle(@NonNull ConnectorMessageResolvedEvent event) {
        cleanup.execute(event.messageIdentifier());
    }
}
