/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.message;

import eu.ecodex.connector.domain.model.message.ConnectorMessageDirection;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import eu.ecodex.connector.domain.model.stats.ConnectorMessageStats;
import eu.ecodex.connector.domain.model.stats.report.ConnectorMessageReportExportFormat;
import eu.ecodex.connector.domain.model.stats.report.summary.ConnectorMessageReportSummary;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.ConnectorOutboundMessageDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.message.ConnectorMessageDetailDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.message.ConnectorMessageDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.transport.ConnectorMessageTransportStepDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.message.c2ctest.ConnectorTestMessageRequest;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Defines the API for managing messages within the connector system for administrative purposes.
 *
 * <p>The base URI for the endpoints defined in this interface is:
 * {@code /api/v1/admin/messages}.
 */
@Tag(
    name = "MessageAdmin",
    description = "API for managing messages within the connector system for administrative "
        + "purposes"
)
@RequestMapping("/api/v1/admin/messages")
public interface ConnectorMessageAdminApi {
    @Hidden
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/outbound/test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a test message from this connector to another connector")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    ConnectorOutboundMessageDto submitOutboundC2CTestMessage(
        @Valid @ModelAttribute ConnectorTestMessageRequest request
    ) throws IOException;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    @Operation(summary = "Get paginated list of messages.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ConnectorPageResult<ConnectorMessageDto> listMessages(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "identifier", required = false) String identifier,
        @RequestParam(name = "backendName", required = false) String backendName,
        @RequestParam(name = "direction", required = false) ConnectorMessageDirection direction,
        @RequestParam(name = "businessDomain", required = false) String businessDomain,
        @RequestParam(name = "service", required = false) String service,
        @RequestParam(name = "action", required = false) String action,
        @RequestParam(name = "from", required = false) Instant from,
        @RequestParam(name = "to", required = false) Instant to
    );

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{identifier}")
    @Operation(summary = "Get a message by identifier.")
    @ApiResponse(responseCode = "200", description = "Message found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ConnectorMessageDetailDto retrieveMessage(@PathVariable String identifier);

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{identifier}/transport-steps")
    @Operation(summary = "Get a message transport step by identifier.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ConnectorMessageTransportStepDto retrieveMessageTransportStep(@PathVariable String identifier);

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/stats")
    @Operation(summary = "Get message statistics.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ConnectorMessageStats getStats(
        @RequestParam(name = "from", required = false) Instant from,
        @RequestParam(name = "to", required = false) Instant to,
        @RequestParam(name = "businessDomain", required = false) String businessDomain
    );

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/reports")
    @Operation(summary = "Get message reporting.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ConnectorMessageReportSummary getReports(
        @RequestParam(name = "from", required = false) Instant from,
        @RequestParam(name = "to", required = false) Instant to,
        @RequestParam(name = "businessDomain", required = false) String businessDomain
    );

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/reports/export")
    @Operation(summary = "Export message reporting.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ResponseEntity<byte[]> exportReports(
        @RequestParam(name = "from", required = false) Instant from,
        @RequestParam(name = "to", required = false) Instant to,
        @RequestParam(name = "businessDomain", required = false) String businessDomain,
        @RequestParam(name = "format") ConnectorMessageReportExportFormat format
    );
}
