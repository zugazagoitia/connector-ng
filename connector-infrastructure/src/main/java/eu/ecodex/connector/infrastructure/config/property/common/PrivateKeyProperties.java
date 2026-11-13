/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.common;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties used to access a private key within a store.
 *
 * <p>This class contains the alias identifying the private key entry and the password required to
 * retrieve the private key from the store.
 */
@Getter
@Setter
public class PrivateKeyProperties {
    @NotBlank
    private String alias;

    @NotBlank
    private String password;

    @Override
    public String toString() {
        return String.format("{alias=%s}", alias);
    }
}
