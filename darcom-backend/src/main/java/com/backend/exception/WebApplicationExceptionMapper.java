package com.backend.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Keeps the original status; maps it to a generic code. */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException e) {
        int status = e.getResponse() != null ? e.getResponse().getStatus() : 500;
        String code = switch (status) {
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> ErrorCode.ERROR;
        };
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            Response.Status reason = Response.Status.fromStatusCode(status);
            message = reason != null ? reason.getReasonPhrase() : "Error";
        }
        String path = uriInfo != null ? uriInfo.getRequestUri().getPath() : "";
        ApiError body = ApiError.of(status, code, message, path);
        return Response.status(status).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
