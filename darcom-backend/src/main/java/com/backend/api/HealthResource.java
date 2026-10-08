package com.backend.api;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** GET /health — unauthenticated liveness probe for uptime checks and deploy platforms. */
@Path("/health")
@Tag(name = "Health", description = "Liveness probe")
public class HealthResource {

    public record HealthResponse(String status) {
    }

    @GET
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Liveness probe")
    @ApiResponse(responseCode = "200", description = "Service is up")
    public Response health() {
        return Response.ok(new HealthResponse("UP")).build();
    }
}
