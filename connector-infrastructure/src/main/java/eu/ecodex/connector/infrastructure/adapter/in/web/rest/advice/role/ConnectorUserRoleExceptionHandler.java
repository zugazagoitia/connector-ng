/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.role;

import eu.ecodex.connector.application.exception.role.ConnectorRoleAlreadyExistsException;
import eu.ecodex.connector.application.exception.role.ConnectorRoleIdentifierException;
import eu.ecodex.connector.application.exception.role.ConnectorRoleInUseException;
import eu.ecodex.connector.application.exception.role.ConnectorRoleNotFoundException;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Connector User Role exception handler for handling exceptions thrown across the application. It
 * provides centralized exception handling using Spring's @RestControllerAdvice and
 *
 * @ExceptionHandler annotations.
 */
@Hidden
@RestControllerAdvice
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ConnectorUserRoleExceptionHandler {
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorRoleInUseException.class)
    public ErrorResponse handleUserRoleDeleteException(ConnectorRoleInUseException e) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorRoleAlreadyExistsException.class)
    public ErrorResponse handleUserRoleIdentifierException(
        ConnectorRoleAlreadyExistsException exception) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), exception.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ConnectorRoleNotFoundException.class)
    public ErrorResponse handleUserRoleNotFoundException(
        ConnectorRoleNotFoundException exception) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorRoleIdentifierException.class)
    public ErrorResponse handleUserRoleException(ConnectorRoleIdentifierException e) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage());
    }
}
