/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user;

import eu.ecodex.connector.application.port.api.auth.user.ConnectorEditUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.NonNull;


/**
 * Represents a request by a user to edit his own account.
 *
 * <p>The {@code email} value is the new email address; it must be a valid email address of at most
 * 255 characters if provided, and may be {@code null} to leave the current email unchanged.
 */
@Builder(toBuilder = true)
public record ConnectorEditSelfRequest(
    @Email(message = "The [email] must be a valid email address")
    @Size(max = 255, message = "The [email] must not exceed 255")
    String email
) {
    /**
     * Maps a self-edit request to an edit user command.
     *
     * @param identifier  the unique identifier of the user to edit (must not be null)
     * @param userRequest the {@link ConnectorEditSelfRequest} carrying the new values (must not be
     *                    null)
     *
     * @return the {@link ConnectorEditUserCommand} built from the identifier and the request
     */
    public static ConnectorEditUserCommand toCommand(
        @NonNull String identifier,
        @NonNull ConnectorEditSelfRequest userRequest) {
        var builder = ConnectorEditUserCommand.builder();
        builder.uuid(identifier);

        if (userRequest.email() != null) {
            builder.email(userRequest.email());
        }
        return builder.build();
    }
}
