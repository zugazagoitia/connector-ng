/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.user;

import eu.ecodex.connector.application.exception.user.ConnectorUserAccountInactiveException;
import eu.ecodex.connector.application.exception.user.ConnectorUserAlreadyExistsException;
import eu.ecodex.connector.application.exception.user.ConnectorUserBadCredentialsException;
import eu.ecodex.connector.application.exception.user.ConnectorUserIdentifierMismatchException;
import eu.ecodex.connector.application.exception.user.ConnectorUserInvalidPasswordException;
import eu.ecodex.connector.application.exception.user.ConnectorUserNotFoundException;
import eu.ecodex.connector.application.exception.user.ConnectorUserPasswordUpdateNotRequiredException;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Connector user exception handler for handling exceptions thrown across the application. It
 * provides centralized exception handling using Spring's @RestControllerAdvice and
 *
 * @ExceptionHandler annotations.
 */
@Hidden
@RestControllerAdvice
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ConnectorUserExceptionHandler {
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorUserPasswordUpdateNotRequiredException.class)
    public ErrorResponse handleUserPasswordException(
        ConnectorUserPasswordUpdateNotRequiredException e) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConnectorUserInvalidPasswordException.class)
    public ErrorResponse handleUserPasswordException(ConnectorUserInvalidPasswordException e) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(ConnectorUserAccountInactiveException.class)
    public ErrorResponse handleDisabledUserAccount(
        ConnectorUserAccountInactiveException exception) {
        return new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(ConnectorUserBadCredentialsException.class)
    public ErrorResponse handleInvalidCredentials(ConnectorUserBadCredentialsException exception) {
        return new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorUserAlreadyExistsException.class)
    public ErrorResponse handleUserIdentifierException(
        ConnectorUserAlreadyExistsException exception) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), exception.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ConnectorUserNotFoundException.class)
    public ErrorResponse handleUserNotFoundException(ConnectorUserNotFoundException exception) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConnectorUserIdentifierMismatchException.class)
    public ErrorResponse handleUserException(ConnectorUserIdentifierMismatchException e) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage());
    }
}
