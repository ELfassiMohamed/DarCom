package com.backend.exception;

import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Bean-validation failures → 400 {@code VALIDATION_ERROR}.
 * High precedence so jersey-bean-validation's built-in mapper does not bypass the envelope.
 */
@Provider
@Priority(1)
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .sorted(Comparator.comparing(v -> propertyName(v)))
                .map(v -> propertyName(v) + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        String path = uriInfo != null ? uriInfo.getRequestUri().getPath() : "";
        ApiError body = ApiError.of(400, ErrorCode.VALIDATION_ERROR, message, path);
        return Response.status(400).entity(body).type(MediaType.APPLICATION_JSON).build();
    }

    /** Strips the method/argument prefix (e.g. {@code login.arg0.email} → {@code email}). */
    static String propertyName(ConstraintViolation<?> v) {
        String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }
}
