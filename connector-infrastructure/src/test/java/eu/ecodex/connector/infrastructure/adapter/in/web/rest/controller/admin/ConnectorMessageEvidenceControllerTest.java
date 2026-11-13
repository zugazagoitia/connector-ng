/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.ecodex.connector.EvidenceTestFixtures;
import eu.ecodex.connector.application.exception.evidence.ConnectorEvidenceNotFoundException;
import eu.ecodex.connector.application.port.api.evidence.ConnectorRetrieveEvidence;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.AbstractWebMvcTest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.evidence.ConnectorEvidenceAdminController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConnectorEvidenceAdminController.class)

@DisplayName("ConnectorMessageEvidenceController")
public class ConnectorMessageEvidenceControllerTest extends AbstractWebMvcTest {
    private static final String URL_DOWNLOAD = "/api/v1/admin/evidences/%s/download";
    private static final String EVIDENCE_UUID = "12345678-1234-1234-1234-123456789012";

    @MockitoBean
    private ConnectorRetrieveEvidence downloadEvidence;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_200_with_the_evidence() throws Exception {
        when(downloadEvidence.execute(any()))
            .thenReturn(EvidenceTestFixtures.createSubmissionAcceptanceEvidence());

        mockMvc.perform(get(URL_DOWNLOAD.formatted(EVIDENCE_UUID))
                            .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_XML));

        verify(downloadEvidence).execute(EVIDENCE_UUID);
    }

    @Test
    void should_return_404_when_the_evidence_is_not_found() throws Exception {
        doThrow(ConnectorEvidenceNotFoundException.class).when(downloadEvidence).execute(any());

        mockMvc.perform(get(URL_DOWNLOAD.formatted("unknown-identifier"))
                            .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
