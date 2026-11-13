/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.dss;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.support.CronExpression;

/**
 * Configuration properties for DSS cache.
 */
@Getter
@Setter
public class DssCacheProperties {
    private static final String DEFAULT_LOCATION =
        System.getProperty("java.io.tmpdir") + "/dss-cache";

    @NotNull
    private Resource location = new FileSystemResource(DEFAULT_LOCATION);

    @NotNull
    private Duration expiration = Duration.ofDays(1);

    @NotBlank
    private String refreshCron = "0 0 */6 * * *";

    @AssertTrue(message = "expiration must be positive")
    public boolean isExpirationPositive() {
        return expiration == null || (!expiration.isZero() && !expiration.isNegative());
    }

    @AssertTrue(message = "refresh-cron must be a valid Spring cron expression")
    public boolean isRefreshCronValid() {
        return refreshCron == null || CronExpression.isValidExpression(refreshCron);
    }
}
