package com.backend.api;

import com.backend.exception.ApiException;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ApiException exception) {
        ErrorEnvelope body = new ErrorEnvelope(
                Instant.now().toString(),
                exception.getStatus(),
                exception.getErrorCode(),
                exception.getMessage(),
                uriInfo.getPath()
        );
        return Response.status(exception.getStatus())
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Matches API-Specification.md §1's error envelope as read while building
     * Task 01 — (timestamp, status, error, message, path). Confirm the field
     * names against the actual spec file before treating this as final; it
     * wasn't re-read for this task the way it was for Tasks 01-03.
     */
    public record ErrorEnvelope(String timestamp, int status, String error, String message, String path) {
    }
}
