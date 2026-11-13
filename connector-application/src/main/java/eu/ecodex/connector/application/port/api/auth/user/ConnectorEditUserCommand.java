/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.port.api.auth.user;

import eu.ecodex.connector.domain.model.user.ConnectorUser;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.util.StringUtils;

/**
 * Represents data used to update user information.
 */
@Builder
public record ConnectorEditUserCommand(
    @NotBlank String uuid,
    String email,
    Boolean enabled,
    Boolean mustChangePassword
) {

    /**
     * create a domain user from edit user command.
     *
     * @param editUserCommand edit user command
     *
     * @return domain user
     */
    public static ConnectorUser toDomain(ConnectorEditUserCommand editUserCommand) {
        var builder = ConnectorUser.builder();
        if (StringUtils.hasText(editUserCommand.email())) {
            builder.email(editUserCommand.email());
        }
        return builder
            .uuid(editUserCommand.uuid())
            .build();
    }
}
