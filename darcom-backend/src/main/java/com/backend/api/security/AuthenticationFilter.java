package com.backend.api.security;

import com.backend.domain.User;
import com.backend.exception.UnauthorizedException;
import com.backend.repository.UserRepository;
import com.backend.security.JwtService;
import com.backend.service.TransactionRunner;
import io.jsonwebtoken.JwtException;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Jersey-served paths that are public by nature, not by annotation: the
     * OpenAPI document comes from a third-party resource class we cannot put
     * @PermitAll on, and the Swagger UI fetches it tokenless from the browser.
     * Exact-match allowlist (not a prefix) so nothing else can slip through.
     */
    private static final List<String> PUBLIC_DOC_PATHS = List.of("openapi.json", "openapi.yaml");

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // Spec §1: Bearer endpoints 401 on a missing token. Public endpoints
            // carry @PermitAll and keep the old pass-through behavior.
            if (isPublicEndpoint() || isPublicDocPath(requestContext)) {
                return;
            }
            throw new UnauthorizedException("UNAUTHENTICATED", "Authentication required");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        UUID userId;
        try {
            userId = JwtService.verify(token);
        } catch (JwtException e) {
            throw new UnauthorizedException("INVALID_TOKEN", "Access token is invalid or expired");
        }

        User user = TransactionRunner.call(em -> new UserRepository(em).findById(userId).orElse(null));
        if (user == null || user.isBlocked()) {
            throw new UnauthorizedException("INVALID_TOKEN", "Access token no longer valid");
        }

        requestContext.setSecurityContext(
                new UserSecurityContext(user, requestContext.getSecurityContext().isSecure()));
    }

    private boolean isPublicDocPath(ContainerRequestContext requestContext) {
        return PUBLIC_DOC_PATHS.contains(requestContext.getUriInfo().getPath());
    }

    private boolean isPublicEndpoint() {
        if (resourceInfo == null) {
            return false;
        }
        Method method = resourceInfo.getResourceMethod();
        if (method != null && method.isAnnotationPresent(PermitAll.class)) {
            return true;
        }
        Class<?> resourceClass = resourceInfo.getResourceClass();
        return resourceClass != null && resourceClass.isAnnotationPresent(PermitAll.class);
    }
}
