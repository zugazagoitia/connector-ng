/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.routing;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents properties defining backend routing rules in the connector configuration.
 *
 * <p>This class is used to define individual backend routing rules, specifying the
 * link partner name and its associated match clause. These properties are used in the broader
 * context of {@link ConnectorMessageRoutingProperties} to configure message routing for the
 * connector.
 */
@Getter
@Setter
public class BackendRuleProperties {
    @NotBlank
    private String linkName;

    private String description;

    @NotBlank
    private String matchClause;
}
