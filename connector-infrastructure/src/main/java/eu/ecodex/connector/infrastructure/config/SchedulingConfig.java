/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config;

import eu.ecodex.connector.infrastructure.adapter.in.scheduler.ConnectorOrphanedAttachmentsCleanupJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class enabling Spring's scheduled task execution (see {@link EnableScheduling}),
 * which activates the {@code @Scheduled} jobs of the connector, such as
 * {@link ConnectorOrphanedAttachmentsCleanupJob}.
 *
 * <p>Scheduling is enabled by default. It can be disabled by setting the property
 * {@code connector.scheduling.enabled} to {@code false}, for example on instances that must not run
 * the jobs or in tests. When the property is missing, scheduling stays enabled.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "connector.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulingConfig {
}
