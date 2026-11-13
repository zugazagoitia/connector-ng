/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.config.property.link;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;

/**
 * Detailed configuration properties for a link partner.
 *
 * <p>These properties are typically used in backend configurations and define endpoint and
 * security-related settings required to communicate with the partner system.
 */
@Getter
@Setter
public class LinkPartnerDetailProperties {
    @NotBlank
    private String encryptionAlias;

    @NotBlank
    private String certificateDn;

    @NotNull
    private URI pushAddress;

    /**
     * Validates that the push address, when present, is an absolute {@code http} or {@code https}
     * URL with a host.
     *
     * <p>A {@code null} address is considered valid here, since {@link NotNull} on
     * {@link #pushAddress} already reports it.
     *
     * @return {@code true} if the push address is {@code null} or an absolute http(s) URL with a
     *     host, {@code false} otherwise
     */

    @AssertTrue(message = "push-address must be an absolute http(s) URL")
    public boolean isPushAddressValid() {
        return pushAddress == null
            || (pushAddress.getHost() != null
            && ("http".equalsIgnoreCase(pushAddress.getScheme())
            || "https".equalsIgnoreCase(pushAddress.getScheme())));
    }
}
