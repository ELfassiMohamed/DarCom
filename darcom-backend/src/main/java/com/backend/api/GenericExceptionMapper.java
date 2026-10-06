package com.backend.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Catches anything ApiExceptionMapper doesn't — JAX-RS picks the most specific
 * mapper for a given exception type, so an ApiException always goes to the
 * other one; this is only ever reached by something genuinely unexpected.
 *
 * NOTE (deploy-driven deviation from TASK-05 §6 verbatim): a
 * WebApplicationException already carries its own Response (e.g. the 403
 * RolesAllowedDynamicFeature throws for a wrong-role caller, or a 404) — it
 * is passed through untouched instead of being flattened to 500. Without this,
 * registering ExceptionMapper<Throwable> hijacks every framework status, and
 * the task's own "403 if it's a VISITOR" check comes back 500. */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webException) {
            return webException.getResponse();
        }

        LOGGER.log(Level.SEVERE, "Unhandled exception on " + uriInfo.getPath(), exception);

        ApiExceptionMapper.ErrorEnvelope body = new ApiExceptionMapper.ErrorEnvelope(
                Instant.now().toString(), 500, "INTERNAL_ERROR", "Something went wrong", uriInfo.getPath());

        return Response.status(500).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
