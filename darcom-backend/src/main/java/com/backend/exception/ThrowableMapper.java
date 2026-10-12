package com.backend.exception;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Catch-all → 500 {@code INTERNAL_ERROR} with a generic message; stack trace only in server logs. */
@Provider
public class ThrowableMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ThrowableMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable e) {
        LOG.log(Level.SEVERE, "Unhandled error", e);
        String path = uriInfo != null ? uriInfo.getRequestUri().getPath() : "";
        ApiError body = ApiError.of(500, ErrorCode.INTERNAL_ERROR, "Internal server error", path);
        return Response.status(500).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
