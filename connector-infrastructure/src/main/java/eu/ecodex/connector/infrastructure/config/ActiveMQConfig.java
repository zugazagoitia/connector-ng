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

import eu.ecodex.connector.infrastructure.config.property.ActiveMQAddressSettingsProperties;
import eu.ecodex.connector.infrastructure.config.property.ConnectorQueueProperties;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.region.policy.RedeliveryPolicyMap;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.boot.activemq.autoconfigure.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ActiveMQConfig class for configuring the ActiveMQ connection factory.
 */
@Configuration
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ActiveMQConfig {
    @Bean
    public ActiveMQConnectionFactoryCustomizer connectionFactoryCustomizer(
        ActiveMQAddressSettingsProperties settingsProperties,
        ConnectorQueueProperties queueProperties) {

        return factory -> {
            factory.setRedeliveryPolicy(defaultRedeliveryPolicy(settingsProperties));

            var policyMap = new RedeliveryPolicyMap();

            addQueue(
                policyMap, queueProperties.outboundMessageStagingQueue(),
                settingsProperties
            );
            addQueue(
                policyMap, queueProperties.outboundMessageProcessingQueue(),
                settingsProperties
            );

            factory.setRedeliveryPolicyMap(policyMap);
        };
    }

    private void addQueue(
        RedeliveryPolicyMap policyMap,
        String queueName,
        ActiveMQAddressSettingsProperties settingsProperties) {
        policyMap.put(new ActiveMQQueue(queueName), defaultRedeliveryPolicy(settingsProperties));
    }

    private RedeliveryPolicy defaultRedeliveryPolicy(
        ActiveMQAddressSettingsProperties settingsProperties) {
        var delayMillis = settingsProperties.redeliveryDelay().toMillis();
        var attempts = settingsProperties.maxDeliveryAttempts();

        var policy = new RedeliveryPolicy();
        policy.setMaximumRedeliveries(attempts - 1);
        policy.setInitialRedeliveryDelay(delayMillis);
        policy.setRedeliveryDelay(delayMillis);
        policy.setBackOffMultiplier(settingsProperties.redeliveryMultiplier());
        policy.setUseExponentialBackOff(settingsProperties.redeliveryMultiplier() > 1.0);

        return policy;
    }
}
