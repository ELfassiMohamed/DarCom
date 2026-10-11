package com.backend.exception;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Renders {@link ApiException} as the error envelope with its own status/code. */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ApiException e) {
        String path = uriInfo != null ? uriInfo.getRequestUri().getPath() : "";
        ApiError body = ApiError.of(e.getStatus(), e.getCode(), e.getMessage(), path);
        return Response.status(e.getStatus()).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
