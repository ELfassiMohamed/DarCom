package com.backend.exception;

import com.fasterxml.jackson.core.JsonParseException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Jackson parse errors → 400 {@code VALIDATION_ERROR}.
 * Registered explicitly (with high precedence) so Jersey's built-in mapper
 * for this type does not bypass the envelope.
 */
@Provider
@Priority(1)
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(JsonParseException e) {
        String path = uriInfo != null ? uriInfo.getRequestUri().getPath() : "";
        ApiError body = ApiError.of(400, ErrorCode.VALIDATION_ERROR, "Malformed request body", path);
        return Response.status(400).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
