/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.message;

import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.ConnectorOutboundMessageDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.message.ConnectorEvidenceMessageDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.message.evidence.ConnectorEvidenceTriggerMessageRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.message.outbound.ConnectorOutboundMessageRequest;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Defines the REST API for managing connector messages.
 */
@Hidden
@Tag(name = "Message", description = "API for managing message.")
@RequestMapping("/api/v1/messages")
public interface ConnectorMessageApi {
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/outbound", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a message from the backend system to the connector")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    ConnectorOutboundMessageDto submitOutboundMessage(
        @Valid @ModelAttribute ConnectorOutboundMessageRequest request
    ) throws IOException;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/evidence-trigger")
    @Operation(summary = "Submit an evidence trigger message from the backend to the connector")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @ApiResponse(responseCode = "409", description = "Conflict")
    ConnectorEvidenceMessageDto submitEvidenceTriggerMessage(
        @Valid @RequestBody ConnectorEvidenceTriggerMessageRequest request);
}
