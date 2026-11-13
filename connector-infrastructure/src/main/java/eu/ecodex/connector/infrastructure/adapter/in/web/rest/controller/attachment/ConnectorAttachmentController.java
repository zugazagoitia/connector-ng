/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.attachment;

import eu.ecodex.connector.application.port.api.attachment.ConnectorUploadAttachments;
import eu.ecodex.connector.application.port.api.attachment.FileUploadCommand;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.exception.ConnectorInternalServerException;
import eu.ecodex.connector.infrastructure.helper.ConnectorTempFilesHelper;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Defines the REST controller for managing large files within the connector system.
 */
@RestController
public class ConnectorAttachmentController implements ConnectorAttachmentApi {
    private final ConnectorUploadAttachments uploadAttachments;
    private final ConnectorTempFilesHelper tempFilesHelper;

    public ConnectorAttachmentController(
        ConnectorUploadAttachments uploadAttachments, ConnectorTempFilesHelper tempFilesHelper) {
        this.uploadAttachments = uploadAttachments;
        this.tempFilesHelper = tempFilesHelper;
    }

    @Override
    public List<String> uploadAttachment(List<MultipartFile> attachments) {
        var fileUploadCommands = attachments
            .stream()
            .map(attachment -> {
                     try {
                         var filename = StringUtils.cleanPath(
                             Objects.requireNonNull(attachment.getOriginalFilename())
                         );

                         var tempLocation = tempFilesHelper.create(
                             "upload-attachment-%s-",
                             filename
                         );
                         attachment.transferTo(tempLocation);

                         var contentType = attachment.getContentType() != null
                             ? attachment.getContentType()
                             : MediaType.APPLICATION_OCTET_STREAM_VALUE;

                         return new FileUploadCommand(
                             filename,
                             attachment.getSize(),
                             contentType,
                             tempLocation,
                             "Uploaded attachment"
                         );
                     } catch (IOException e) {
                         throw new ConnectorInternalServerException(e.getMessage());
                     }
                 }
            ).toList();

        try {
            return this.uploadAttachments.execute(fileUploadCommands)
                                         .stream()
                                         .map(ConnectorMessageAttachment::identifier)
                                         .toList();
        } finally {
            fileUploadCommands.forEach(FileUploadCommand::cleanup);
        }
    }
}
