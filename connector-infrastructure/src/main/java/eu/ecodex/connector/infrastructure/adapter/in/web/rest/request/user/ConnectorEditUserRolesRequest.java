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

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;

/**
 * This Dto class is used to target user roles update.
 *
 * <p>The {@code roles} indicates the roles to set to the user
 */
@Builder(toBuilder = true)
public record ConnectorEditUserRolesRequest(
    @NonNull
    @NotEmpty(message = "The [roles] must not be empty")
    Set<String> roles) {
}
