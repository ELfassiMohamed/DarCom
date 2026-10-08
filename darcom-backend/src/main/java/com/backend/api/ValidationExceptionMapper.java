package com.backend.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));

        ApiExceptionMapper.ErrorEnvelope body = new ApiExceptionMapper.ErrorEnvelope(
                Instant.now().toString(), 400, "VALIDATION_ERROR", message, uriInfo.getPath());

        return Response.status(400).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
