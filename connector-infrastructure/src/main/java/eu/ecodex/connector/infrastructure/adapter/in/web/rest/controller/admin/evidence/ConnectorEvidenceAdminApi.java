/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.evidence;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Defines the REST API for managing connector messages evidence.
 */
@Tag(
    name = "EvidenceAdmin",
    description = "API for managing message evidence for administrative purposes."
)
@RequestMapping("/api/v1/admin/evidences")
public interface ConnectorEvidenceAdminApi {
    @ResponseStatus(HttpStatus.CREATED)
    @GetMapping(value = "/{uuid}/download")
    @Operation(summary = "Download a message evidence by its uuid")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ResponseEntity<byte[]> downloadEvidence(@PathVariable String uuid) throws IOException;
}
