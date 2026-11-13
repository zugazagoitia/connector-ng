/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.ecodex.connector.FileTestFixtures;
import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.application.port.api.attachment.ConnectorUploadAttachments;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.attachment.ConnectorAttachmentController;
import eu.ecodex.connector.infrastructure.helper.ConnectorTempFilesHelper;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConnectorAttachmentController.class)
@DisplayName("ConnectorAttachmentController")
public class ConnectorAttachmentControllerTest extends AbstractWebMvcTest {
    private static final String URL = "/api/v1/attachments/upload";

    @MockitoBean
    private ConnectorUploadAttachments uploadAttachments;
    @MockitoBean
    private ConnectorTempFilesHelper tempFilesHelper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_201_when_uploading_attachments() throws Exception {
        when(uploadAttachments.execute(any()))
            .thenReturn(List.of(MessageAttachmentTestFixtures.createAttachment()));
        when(tempFilesHelper.create(anyString(), anyString()))
            .thenAnswer(invocation -> Files.createTempFile("test-attachment-", ".tmp"));

        mockMvc.perform(multipart(HttpMethod.POST, URL)
                            .file(getAttachment("raw/fake_file.pdf", "fake_file.pdf"))
                            .file(getAttachment("raw/fake_file.txt", "fake_file.txt"))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
               .andExpect(status().isCreated())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$", hasSize(1)));
    }

    private MockMultipartFile getAttachment(String resourcePath, String filename) {
        return new MockMultipartFile(
            "attachments",
            filename,
            MediaType.APPLICATION_XML_VALUE,
            FileTestFixtures.readAsBytes(resourcePath)
        );
    }
}
