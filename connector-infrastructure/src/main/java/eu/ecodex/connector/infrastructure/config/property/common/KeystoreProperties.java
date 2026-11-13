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

import eu.ecodex.connector.domain.model.security.KeystoreType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties used for accessing a secure store.
 *
 * <p>This class holds the file system path to the store and the password required to access it.
 */
@Getter
@Setter
public class KeystoreProperties {
    @NotBlank
    private String path;

    @NotBlank
    private String password;

    @NotNull
    private KeystoreType type = KeystoreType.JKS;

    @Override
    public String toString() {
        return String.format("{path=%s, type=%s}", path, type);
    }
}
