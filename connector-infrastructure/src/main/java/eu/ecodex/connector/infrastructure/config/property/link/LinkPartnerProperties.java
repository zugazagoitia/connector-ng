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

import eu.ecodex.connector.domain.model.link.ConnectorLinkMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties describing a link partner.
 *
 * <p>A link partner represents a remote system or integration endpoint that exchanges messages
 * with the application. The configuration defines identification, operational modes, and optional
 * settings depending on whether the partner is used by the gateway or backend.
 */
@Getter
@Setter
public class LinkPartnerProperties {
    @NotBlank
    private String name;

    private String description;

    private boolean enabled;

    private ConnectorLinkMode senderMode;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    private LinkPartnerDetailProperties properties; // backend only

    /**
     * Validates that a push address is configured when the sender mode is push.
     *
     * <p>The check only applies when {@code senderMode} is {@link ConnectorLinkMode#PUSH}: in that
     * mode, the connector pushes messages to the partner, so the partner's detailed properties and
     * their push address are required. For any other sender mode, nothing is required.
     *
     * <p>This only checks that the address is present. Its format (an absolute http(s) URL) is
     * validated by {@link LinkPartnerDetailProperties#isPushAddressValid()}, so the details must be
     * validated recursively with {@code @Valid} on the {@code properties} field.
     *
     * @return {@code true} if the sender mode is not push, or if the detailed properties and their
     *     push address are present, {@code false} otherwise
     */
    @AssertTrue(message = "properties.push-address is required when sender-mode is push")
    public boolean isPushAddressConfigured() {
        return senderMode != ConnectorLinkMode.PUSH
            || (properties != null && properties.getPushAddress() != null);
    }
}
