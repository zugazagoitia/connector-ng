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

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the default activemq settings.
 */
@Validated
@ConfigurationProperties(prefix = "connector.activemq.address-settings")
public record ActiveMQAddressSettingsProperties(
    @Min(1)
    @DefaultValue("5")
    int maxDeliveryAttempts,

    @NotNull
    @DefaultValue("60s")
    Duration redeliveryDelay,

    @DecimalMin("1.0")
    @DefaultValue("2.0")
    double redeliveryMultiplier
) {
    @AssertTrue(message = "redelivery-delay must be positive")
    public boolean isRedeliveryDelayPositive() {
        return redeliveryDelay == null
            || (!redeliveryDelay.isZero() && !redeliveryDelay.isNegative());
    }
}
