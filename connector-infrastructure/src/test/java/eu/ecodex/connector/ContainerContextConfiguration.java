/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector;

import eu.ecodex.connector.infrastructure.config.BeanConfig;
import eu.ecodex.connector.infrastructure.config.DatabaseConfig;
import eu.ecodex.connector.infrastructure.config.DssConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;


@Import({DssConfig.class, DatabaseConfig.class, BeanConfig.class})
@ComponentScan(
    basePackages = {
        "eu.ecodex.connector.domain",
        "eu.ecodex.connector.infrastructure.helper",
        "eu.ecodex.connector.infrastructure.config.property",
        "eu.ecodex.connector.infrastructure.adapter.out.provider",
        "eu.ecodex.connector.infrastructure.adapter.out.dss",
        "eu.ecodex.connector.infrastructure.adapter.out.security",
        "eu.ecodex.connector.infrastructure.adapter.out.persistence.db",
        "eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter"
    }
)
public class ContainerContextConfiguration extends SecurityDisabledTestConfiguration {
}
