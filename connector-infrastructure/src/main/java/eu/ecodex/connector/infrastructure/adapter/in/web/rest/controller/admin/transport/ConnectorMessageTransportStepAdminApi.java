/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.transport;

import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.message.ConnectorMessageDetailDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.transport.ConnectorMessageTransportStepDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Defines the REST API for managing message transport steps.
 */
@Tag(
    name = "MessageTransportStepAdmin",
    description = "API for managing message transport steps for administrative purposes."
)
@RequestMapping("/api/v1/admin/transport-steps")
public interface ConnectorMessageTransportStepAdminApi {
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    @Operation(summary = "Get paginated list of messages transport steps.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ConnectorPageResult<ConnectorMessageTransportStepDto> listTransportSteps(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "stepOrMessageOrRemoteSystemIdentifier", required = false)
        String stepOrMessageOrRemoteSystemIdentifier,
        @RequestParam(name = "linkPartnerName", required = false) String linkPartnerName,
        @RequestParam(name = "statuses", required = false)
        List<ConnectorMessageTransportStatus> statuses,
        @RequestParam(name = "from", required = false) Instant from,
        @RequestParam(name = "to", required = false) Instant to
    );

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("{identifier}/message")
    @Operation(summary = "Get transported message.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ConnectorMessageDetailDto retrieveTransportedMessage(@PathVariable String identifier);
}
