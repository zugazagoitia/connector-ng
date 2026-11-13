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

import eu.ecodex.connector.application.port.api.evidence.ConnectorRetrieveEvidence;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Defines the REST controller for managing connector messages evidence.
 */
@RestController
public class ConnectorEvidenceAdminController implements ConnectorEvidenceAdminApi {
    private final ConnectorRetrieveEvidence retrieveEvidence;

    public ConnectorEvidenceAdminController(ConnectorRetrieveEvidence retrieveEvidence) {
        this.retrieveEvidence = retrieveEvidence;
    }

    @Override
    public ResponseEntity<byte[]> downloadEvidence(String uuid) {
        var evidence = retrieveEvidence.execute(uuid);
        var contentLength = evidence.content() != null ? evidence.content().length : 0;

        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_XML)
                             .contentLength(contentLength)
                             .header(
                                 HttpHeaders.CONTENT_DISPOSITION,
                                 "attachment; filename=%s.xml".formatted(evidence.type())
                             )
                             .body(evidence.content());
    }
}
