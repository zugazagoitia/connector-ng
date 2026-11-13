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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.ecodex.connector.MessageAttachmentTestFixtures;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentDeletedException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.application.port.api.attachment.ConnectorDownloadAttachment;
import eu.ecodex.connector.application.port.api.attachment.ConnectorListAttachments;
import eu.ecodex.connector.application.port.api.attachment.ConnectorRetrieveAttachment;
import eu.ecodex.connector.domain.model.paging.ConnectorPageResult;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.AbstractWebMvcTest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.admin.attachment.ConnectorAttachmentAdminController;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorAttachmentAdminController")
@WebMvcTest(ConnectorAttachmentAdminController.class)
public class ConnectorAttachmentAdminControllerTest extends AbstractWebMvcTest {
    @MockitoBean
    private ConnectorListAttachments listAttachments;
    @MockitoBean
    private ConnectorDownloadAttachment downloadAttachment;
    @MockitoBean
    private ConnectorRetrieveAttachment retrieveAttachment;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET (list attachments)")
    class ListAttachments {
        @Test
        void should_return_200_when_retrieving_attachments() throws Exception {
            var pageResult = new ConnectorPageResult<>(
                List.of(MessageAttachmentTestFixtures.createAttachment()), 1, 1, 1
            );

            when(listAttachments.execute(any())).thenReturn(
                pageResult);

            mockMvc.perform(get("/api/v1/admin/attachments")
                                .param("page", "0")
                                .param("size", "20")
                                .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.totalElements").value(1))
                   .andExpect(jsonPath("$.totalPages").value(1))
                   .andExpect(jsonPath("$.size").value(1))
                   .andExpect(jsonPath("$.content").isArray())
                   .andExpect(jsonPath("$.content.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET (download attachment)")
    class DownloadAttachment {
        private static final String IDENTIFIER = "c12f879b-3c9a-4d26-b36c-b6d67a84f0ed_test_attachment";

        @Test
        void should_return_200_when_downloading_the_attachment() throws Exception {
            when(retrieveAttachment.execute(any()))
                .thenReturn(MessageAttachmentTestFixtures.createAttachment());
            when(downloadAttachment.execute(any())).thenReturn(new byte[1]);

            mockMvc.perform(
                       get("/api/v1/admin/attachments/%s/download".formatted(IDENTIFIER))
                           .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isOk());
        }

        @Test
        void should_return_404_when_attachment_not_found() throws Exception {
            doThrow(ConnectorMessageAttachmentNotFoundException.class).when(
                retrieveAttachment).execute(any());

            mockMvc.perform(get("/api/v1/admin/attachments/unknown-id/download")
                                .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        void should_return_409_when_attachment_is_deleted_from_storage() throws Exception {
            doThrow(ConnectorMessageAttachmentDeletedException.class)
                .when(retrieveAttachment).execute(any());

            mockMvc.perform(
                       get("/api/v1/admin/attachments/%s/download".formatted(IDENTIFIER))
                           .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isConflict())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        void should_return_409_when_attachment_is_no_longer_available_in_the_storage()
            throws Exception {
            when(retrieveAttachment.execute(any()))
                .thenReturn(MessageAttachmentTestFixtures.createAttachment());
            doThrow(ConnectorMessageAttachmentException.class).when(retrieveAttachment)
                                                              .execute(any());

            mockMvc.perform(
                       get("/api/v1/admin/attachments/%s/download".formatted(IDENTIFIER))
                           .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
