/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto;

import eu.ecodex.connector.domain.ConnectorDefaults;
import eu.ecodex.connector.domain.model.link.ConnectorConfigurationSource;
import eu.ecodex.connector.domain.model.link.ConnectorLinkMode;
import eu.ecodex.connector.domain.model.link.ConnectorLinkType;
import eu.ecodex.connector.domain.model.link.partner.ConnectorLinkPartner;
import java.net.URI;
import lombok.Builder;

/**
 * Represents a link partner within the connector system, encapsulating the configuration,
 * attributes, and operational characteristics of the link.
 *
 * <p>This record is designed to define the essential properties and configuration for a link
 * partner in the system. It serves to manage the partner details, receiveMode of operation, and
 * associated metadata required for integration and communication purposes.
 *
 * @param name            The name of the link partner.
 * @param description     A textual description of the link partner, providing additional context or
 *                        details.
 * @param enabled         A flag indicating whether the link partner is currently enabled or
 *                        active.
 * @param senderMode      The interaction receiveMode of the link, defined by
 *                        {@link ConnectorLinkMode}, specifying how communication or data transfer
 *                        is managed.
 * @param type            The type of the connector link, categorized by {@link ConnectorLinkType},
 *                        distinguishing backend and gateway link types.
 * @param source          The origin source of the link configuration, defined by
 *                        {@link ConnectorConfigurationSource}, specifying where the configuration
 *                        data is derived from.
 * @param encryptionAlias alias of the encryption key used when communicating with this partner,
 *                        typically referencing a keystore entry
 * @param certificateDn   distinguished name (DN) of the partner certificate used for authentication
 *                        or secure communication
 * @param pushAddress     The backend system address used by the connector to submit messages.
 * @param testOnly        If this action is reserved for test messages and must not be
 *                        used for business messages
 */
@Builder(toBuilder = true)
public record ConnectorLinkPartnerDto(
    String name,
    String description,
    boolean enabled,
    ConnectorLinkMode senderMode,
    ConnectorLinkType type,
    ConnectorConfigurationSource source,
    String encryptionAlias,
    String certificateDn,
    URI pushAddress,
    boolean testOnly
) {
    /**
     * Creates a new instance of {@code ConnectorLinkPartnerDto} from the provided
     * {@link ConnectorLinkPartner}.
     *
     * @param linkPartner the {@code ConnectorLinkPartner} instance containing the link partner's
     *                    configuration and attributes
     *
     * @return a new {@link ConnectorLinkPartnerDto} populated with the corresponding data from the
     *     provided {@link ConnectorLinkPartner}
     */
    public static ConnectorLinkPartnerDto of(ConnectorLinkPartner linkPartner) {
        return new ConnectorLinkPartnerDto(
            linkPartner.name().name(),
            linkPartner.description(),
            linkPartner.enabled(),
            linkPartner.senderMode(),
            linkPartner.type(),
            linkPartner.source(),
            linkPartner.encryptionAlias(),
            linkPartner.certificateDn(),
            linkPartner.pushAddress(),
            linkPartner.name().name().equals(ConnectorDefaults.DEFAULT_TEST_BACKEND_NAME)
        );
    }
}
