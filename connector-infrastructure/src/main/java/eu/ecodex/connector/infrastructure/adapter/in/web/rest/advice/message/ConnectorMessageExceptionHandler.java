/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.message;

import eu.ecodex.connector.application.exception.businessdomain.ConnectorBusinessDomainAlreadyExistsException;
import eu.ecodex.connector.application.exception.businessdomain.ConnectorBusinessDomainNotFoundException;
import eu.ecodex.connector.application.exception.c2ctest.ConnectorC2CTestMessageDisabledException;
import eu.ecodex.connector.application.exception.evidence.ConnectorEvidenceException;
import eu.ecodex.connector.application.exception.evidence.ConnectorEvidenceNotFoundException;
import eu.ecodex.connector.application.exception.message.ConnectorMessageNotFoundException;
import eu.ecodex.connector.application.exception.message.ConnectorMessageTransportStepNotFoundException;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Connector Message exception handler for handling exceptions thrown across the application. It
 * provides centralized exception handling using Spring's @RestControllerAdvice and
 *
 * @ExceptionHandler annotations.
 */
@Hidden
@RestControllerAdvice
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ConnectorMessageExceptionHandler {
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorBusinessDomainAlreadyExistsException.class)
    public ErrorResponse handleBusinessDomainException(
        ConnectorBusinessDomainAlreadyExistsException exception) {
        return new ErrorResponse(
            HttpStatus.CONFLICT.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorC2CTestMessageDisabledException.class)
    public ErrorResponse handleBTestMessageDisabledException(
        ConnectorC2CTestMessageDisabledException exception) {
        return new ErrorResponse(
            HttpStatus.CONFLICT.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ConnectorBusinessDomainNotFoundException.class)
    public ErrorResponse handleBusinessDomainNotFoundException(
        ConnectorBusinessDomainNotFoundException exception) {
        return new ErrorResponse(
            HttpStatus.NOT_FOUND.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ConnectorMessageNotFoundException.class)
    public ErrorResponse handleMessageNotFoundException(
        ConnectorMessageNotFoundException exception) {
        return new ErrorResponse(
            HttpStatus.NOT_FOUND.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ConnectorMessageTransportStepNotFoundException.class)
    public ErrorResponse handleMessageTransportStepsNotFoundException(
        ConnectorMessageTransportStepNotFoundException exception) {
        return new ErrorResponse(
            HttpStatus.NOT_FOUND.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ConnectorEvidenceNotFoundException.class)
    public ErrorResponse handleMessageEvidenceNotFoundException(
        ConnectorEvidenceNotFoundException exception) {
        return new ErrorResponse(
            HttpStatus.NOT_FOUND.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorEvidenceException.class)
    public ErrorResponse handleConnectorEvidenceException(ConnectorEvidenceException exception) {
        return new ErrorResponse(
            HttpStatus.CONFLICT.value(), exception.getMessage()
        );
    }
}
