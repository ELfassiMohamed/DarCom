package com.backend.api.diagnostic;

import com.backend.api.security.UserSecurityContext;
import com.backend.domain.User;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Throwaway — proves the bootstrap, the filter, and both mappers actually
 * work end to end before Task 06 builds real resources on top of them.
 * Same role PersistenceSmokeCheck played for Task 01-02's persistence layer.
 * Delete the whole com.backend.api.diagnostic package once that's confirmed.
 *
 * NOTE (deploy-driven deviation from TASK-05 §8 verbatim): the resource takes
 * @Context ContainerRequestContext and reads getSecurityContext() off it
 * instead of taking @Context SecurityContext directly. Jersey wraps an
 * injected SecurityContext in its own SecurityContextInjectee delegate, so
 * the §5.2 "one cast" only works on the instance the filter set — which is
 * what getSecurityContext() returns. Same contract, portable API, no
 * Jersey-internal dependency.
 */
@Path("/_diag/whoami")
public class WhoAmIResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WhoAmIResponse whoAmI(@Context ContainerRequestContext requestContext) {
        SecurityContext securityContext = requestContext.getSecurityContext();
        if (securityContext.getUserPrincipal() == null) {
            return new WhoAmIResponse(false, null, null);
        }
        User user = ((UserSecurityContext) securityContext).getUser();
        return new WhoAmIResponse(true, user.getId().toString(), user.getRole().name());
    }

    /** Same as whoAmI, but only reachable with role HOST — proves RolesAllowedDynamicFeature actually rejects the wrong role, not just that a token parses. */
    @GET
    @Path("/host-only")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("HOST")
    public WhoAmIResponse hostOnly(@Context ContainerRequestContext requestContext) {
        User user = ((UserSecurityContext) requestContext.getSecurityContext()).getUser();
        return new WhoAmIResponse(true, user.getId().toString(), user.getRole().name());
    }

    public record WhoAmIResponse(boolean authenticated, String userId, String role) {
    }
}
