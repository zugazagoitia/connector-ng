/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the default connector queues.
 */
@Validated
@SuppressWarnings("checkstyle:LineLength")
@ConfigurationProperties(prefix = "connector.queues")
public record ConnectorQueueProperties(
    @NotBlank
    @DefaultValue("connector.queues.outbound-message-staging-queue")
    String outboundMessageStagingQueue,

    @NotBlank
    @DefaultValue("connector.queues.outbound-message-processing-queue")
    String outboundMessageProcessingQueue,

    @NotBlank
    @DefaultValue("connector.queues.outbound-evidence-trigger-queue")
    String outboundEvidenceTriggerQueue,

    @NotBlank
    @DefaultValue("connector.queues.inbound-message-staging-queue")
    String inboundMessageStagingQueue,

    @NotBlank
    @DefaultValue("connector.queues.inbound-message-processing-queue")
    String inboundMessageProcessingQueue,

    @NotBlank
    @DefaultValue("connector.queues.inbound-evidence-trigger-queue")
    String inboundEvidenceTriggerQueue,

    @NotBlank
    @DefaultValue("connector.queues.backend-delivery-queue")
    String backendDeliveryQueue,

    @NotBlank
    @DefaultValue("domibus.backend.jms.inQueue")
    String gatewaySubmissionQueue,

    @NotBlank
    @DefaultValue("domibus.backend.jms.replyQueue")
    String gatewaySubmissionReplyQueue,

    @NotBlank
    @DefaultValue("connector.queues.resolved-message-cleanup-processing-queue")
    String resolvedMessageCleanupProcessingQueue
) {
}
