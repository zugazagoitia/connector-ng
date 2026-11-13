/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.advice;

import eu.ecodex.connector.infrastructure.adapter.in.web.rest.exception.ConnectorBadRequestException;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.exception.ConnectorInternalServerException;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Global exception handler for handling exceptions thrown across the application. It provides
 * centralized exception handling using Spring's @RestControllerAdvice and @ExceptionHandler
 * annotations.
 */
@Slf4j
@Hidden
@RestControllerAdvice
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class GlobalExceptionHandler {
    private static List<String> allowedValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
            .map(constant -> ((Enum<?>) constant).name())
            .toList();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ErrorResponse handleInvalidEnumeration(MethodArgumentTypeMismatchException e) {
        Class<?> type = e.getRequiredType();
        var message = (type != null && type.isEnum())
            ? "%s must be one of: %s".formatted(e.getName(), allowedValues(type))
            : "Parameter '%s' has an invalid value".formatted(e.getName());
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleInvalidEnumeration(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidFormatException ex && ex.getTargetType().isEnum()) {
            var field = ex.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .collect(Collectors.joining("."));
            var allowedValues = allowedValues(ex.getTargetType());
            return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                field + " must be one of " + allowedValues
            );
        }
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Malformed request body");
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConnectorBadRequestException.class)
    public ErrorResponse handleBadRequestException(ConnectorBadRequestException exception) {
        return new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), exception.getMessage()
        );
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ConnectorInternalServerException.class)
    public ErrorResponse handleInternalServerException(ConnectorInternalServerException exception) {
        return new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage()
        );
    }

    /**
     * Safety reply for a request whose binding crossed a nested object that could not be built.
     *
     * <p>The response is intentionally generic. The details are logged at WARN level so that an
     * unprotected endpoint can be found and added to {@link RecordBindingAdvice}.
     *
     * @param ex the exception raised while binding request values to the model
     *
     * @return a 400 response with a generic message
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NullValueInNestedPathException.class)
    public ErrorResponse handleNestedPath(NullValueInNestedPathException ex) {
        log.warn("Request binding failed on nested path '{}': {}", ex.getPropertyName(),
            ex.getMessage());
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
            "The [" + ex.getPropertyName() + "] is malformed or incomplete");
    }


    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
            .getAllErrors()
            .stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
    }
}
