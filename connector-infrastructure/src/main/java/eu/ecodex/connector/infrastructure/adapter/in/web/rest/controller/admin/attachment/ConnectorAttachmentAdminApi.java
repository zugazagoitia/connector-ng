/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.attachment;

import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.ConnectorAttachmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Defines the API for managing message attachments for administrative purposes.
 *
 * <p>The base URI for the endpoints defined in this interface is:
 * {@code /api/v1/admin/attachments}.
 */
@RequestMapping("/api/v1/admin/attachments")
@Tag(
    name = "MessageAttachmentAdmin",
    description = "Defines the API for managing message attachments for administrative purposes"
)
public interface ConnectorAttachmentAdminApi {
    @GetMapping
    @Operation(summary = "Get paginated list of message attachments.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    ConnectorPageResult<ConnectorAttachmentDto> listAttachments(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "messageIdentifier", required = false) String messageIdentifier,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "types", required = false) List<ConnectorAttachmentType> types,
        @RequestParam(name = "storages", required = false)
        List<ConnectorAttachmentStorage> storages,
        @RequestParam(name = "from", required = false) Instant from,
        @RequestParam(name = "to", required = false) Instant to
    );

    @GetMapping("/{identifier}/download")
    @Operation(summary = "Get a message attachment by identifier.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @ApiResponse(responseCode = "409", description = "Conflict")
    ResponseEntity<byte[]> download(@PathVariable String identifier) throws IOException;
}
