/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.provider;

import eu.ecodex.connector.application.port.spi.ConnectorMessageRoutingConfigurationProvider;
import eu.ecodex.connector.domain.ConnectorDefaults;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomain;
import eu.ecodex.connector.domain.model.link.ConnectorConfigurationSource;
import eu.ecodex.connector.domain.model.link.partner.ConnectorLinkPartnerName;
import eu.ecodex.connector.domain.model.message.routing.ConnectorMessageRoutingBusinessDomainItem;
import eu.ecodex.connector.domain.model.message.routing.ConnectorMessageRoutingBusinessDomainProperties;
import eu.ecodex.connector.domain.model.message.routing.ConnectorMessageRoutingConfiguration;
import eu.ecodex.connector.domain.model.message.routing.ConnectorMessageRoutingRule;
import eu.ecodex.connector.domain.routing.ConnectorRoutingRulePattern;
import eu.ecodex.connector.infrastructure.config.property.c2ctest.Connector2ConnectorTestMessageProperties;
import eu.ecodex.connector.infrastructure.config.property.routing.ConnectorMessageRoutingProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provides the implementation for configuring connector message routing based on defined
 * properties. This class integrates the default backend rules and test message routing rules into a
 * consistent configuration to be utilized within the connector system.
 */
@Slf4j
@Component
public class ConnectorPropertyMessageRoutingConfigurationProvider implements
    ConnectorMessageRoutingConfigurationProvider {
    private final ConnectorMessageRoutingProperties routingProperties;
    private final Connector2ConnectorTestMessageProperties testMessageProperties;

    public ConnectorPropertyMessageRoutingConfigurationProvider(
        ConnectorMessageRoutingProperties routingProperties,
        Connector2ConnectorTestMessageProperties testMessageProperties) {
        this.routingProperties = routingProperties;
        this.testMessageProperties = testMessageProperties;
    }

    @Override
    public ConnectorMessageRoutingConfiguration getConfiguration() {
        // Accumulate all rules under the single default domain before building
        var partnerRoutingRules = new LinkedHashMap<ConnectorLinkPartnerName,
            ConnectorMessageRoutingRule>();

        for (var rule : routingProperties.getBackendRules()) {
            var partnerName = new ConnectorLinkPartnerName(rule.getLinkName());
            var routingRule = ConnectorMessageRoutingRule
                .builder()
                .linkName(rule.getLinkName())
                .description(
                    rule.getDescription() == null
                        ? "Routing rule for backend link %s".formatted(rule.getLinkName())
                        : rule.getDescription()
                )
                .matchClause(new ConnectorRoutingRulePattern(rule.getMatchClause()))
                .build();
            partnerRoutingRules.put(partnerName, routingRule);
        }

        var testMessagePartner = new ConnectorLinkPartnerName(
            ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME
        );

        if (routingProperties.isEnabled()) {
            var testMessageRoutingRule = buildTestMessageRoutingRule();
            partnerRoutingRules.put(testMessagePartner, testMessageRoutingRule);
        }

        var backendRouting = new ConnectorMessageRoutingBusinessDomainItem(
            routingProperties.getDefaultBackendName(),
            Collections.unmodifiableMap(partnerRoutingRules)
        );

        var domainProperties = new ConnectorMessageRoutingBusinessDomainProperties(
            backendRouting,
            null
        );

        var businessDomainRouting = Map.of(
            ConnectorBusinessDomain.DEFAULT_BUSINESS_DOMAIN_ID, domainProperties);

        return new ConnectorMessageRoutingConfiguration(
            routingProperties.isEnabled(),
            businessDomainRouting
        );
    }

    private ConnectorMessageRoutingRule buildTestMessageRoutingRule() {
        log.info("Adding routing rule for Connector2Connector Test message");

        var testService = testMessageProperties.service();
        var action = testMessageProperties.action();

        var pattern = ("|(&(equals(ServiceName, '%s'),equals(ServiceType, '%s')), "
            + "equals(Action, '%s'))").formatted(
            testService.name(), testService.type(), action
        );

        return ConnectorMessageRoutingRule
            .builder()
            .configurationSource(ConnectorConfigurationSource.IMPLEMENTATION)
            .priority(ConnectorMessageRoutingRule.HIGH_PRIORITY)
            .linkName(ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME)
            .description("Routing rule for Connector2Connector Test message")
            .matchClause(new ConnectorRoutingRulePattern(pattern))
            .build();
    }
}
