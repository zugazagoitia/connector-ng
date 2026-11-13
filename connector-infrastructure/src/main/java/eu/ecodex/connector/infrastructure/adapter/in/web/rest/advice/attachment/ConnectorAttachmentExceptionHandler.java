/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.attachment;

import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentDeletedException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentException;
import eu.ecodex.connector.application.exception.attachment.ConnectorMessageAttachmentNotFoundException;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.ErrorResponse;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.exception.ConnectorAttachmentUploadException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Connector Attachment exception handler for handling exceptions thrown across the application. It
 * provides centralized exception handling using Spring's @RestControllerAdvice and
 *
 * @ExceptionHandler annotations.
 */
@Hidden
@RestControllerAdvice
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ConnectorAttachmentExceptionHandler {
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ConnectorMessageAttachmentNotFoundException.class)
    public ErrorResponse handleMessageAttachmentNotFoundException(
        ConnectorMessageAttachmentNotFoundException exception) {
        return new ErrorResponse(
            HttpStatus.NOT_FOUND.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorMessageAttachmentException.class)
    public ErrorResponse handleMessageAttachmentException(
        ConnectorMessageAttachmentException exception) {
        return new ErrorResponse(
            HttpStatus.CONFLICT.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorMessageAttachmentDeletedException.class)
    public ErrorResponse handleMessageAttachmentDeletionException(
        ConnectorMessageAttachmentDeletedException exception) {
        return new ErrorResponse(
            HttpStatus.CONFLICT.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ConnectorAttachmentUploadException.class)
    public ErrorResponse handleAttachmentUploadException(ConnectorAttachmentUploadException e) {
        return new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()
        );
    }
}
