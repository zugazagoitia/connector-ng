/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.deletion;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.scheduling.support.CronExpression;

/**
 * Configuration properties for the file deletion rule.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeletionRule {
    @NotNull
    private Duration threshold;

    @NotBlank
    private String cron;

    @AssertTrue(message = "threshold must be positive")
    public boolean isThresholdPositive() {
        return threshold == null || (!threshold.isZero() && !threshold.isNegative());
    }

    @AssertTrue(message = "cron must be a valid Spring cron expression")
    public boolean isCronValid() {
        return cron == null || CronExpression.isValidExpression(cron);
    }
}
