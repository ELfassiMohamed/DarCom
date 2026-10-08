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
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // Spec §1: Bearer endpoints 401 on a missing token. Public endpoints
            // carry @PermitAll and keep the old pass-through behavior.
            if (isPublicEndpoint()) {
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
