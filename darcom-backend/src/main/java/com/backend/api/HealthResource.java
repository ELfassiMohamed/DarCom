package com.backend.api;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** GET /health — unauthenticated liveness probe for uptime checks and deploy platforms. */
@Path("/health")
public class HealthResource {

    public record HealthResponse(String status) {
    }

    @GET
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(new HealthResponse("UP")).build();
    }
}
