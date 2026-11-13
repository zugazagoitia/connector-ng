/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.pmode;

import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.pmode.ConnectorProcessingModeDetailDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.pmode.ConnectorProcessingModeDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.pmode.ConnectorProcessingModeTruststoreDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.pmode.ConnectorProcessingModeCreationRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.pmode.ConnectorProcessingModeTruststoreRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Defines the API for managing processing modes within the connector system for administrative
 * purposes.
 *
 * <p>The base URI for the endpoints defined in this interface is:
 * {@code /api/v1/admin/processing-modes}.
 */
@Tag(
    name = "ProcessingModeAdmin",
    description = "API for managing processing modes for administrative purposes"
)
@RequestMapping("/api/v1/admin/processing-modes")
public interface ConnectorProcessingModeAdminApi {
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a new processing mode.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    ConnectorProcessingModeDto registerPmode(
        @Valid @ModelAttribute ConnectorProcessingModeCreationRequest request) throws IOException;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get all processing modes.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    List<ConnectorProcessingModeDto> listPmodes();

    @GetMapping("/{uuid}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get a processing mode by identifier.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ConnectorProcessingModeDetailDto retrievePmode(@PathVariable String uuid);

    @GetMapping("/{uuid}/download")
    @Operation(summary = "Download a processing mode")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ResponseEntity<byte[]> downloadPmode(@PathVariable String uuid);

    @PatchMapping(value = "/{uuid}/truststore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update the truststore of a processing mode")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not Found")
    ConnectorProcessingModeTruststoreDto updateTruststore(
        @PathVariable String uuid,
        @Valid @ModelAttribute ConnectorProcessingModeTruststoreRequest request);
}
