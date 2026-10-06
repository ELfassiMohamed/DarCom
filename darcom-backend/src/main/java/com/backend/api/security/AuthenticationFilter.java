package com.backend.api.security;

import com.backend.domain.User;
import com.backend.exception.UnauthorizedException;
import com.backend.repository.UserRepository;
import com.backend.security.JwtService;
import com.backend.service.TransactionRunner;
import io.jsonwebtoken.JwtException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return; // no token — proceed unauthenticated (§3.2)
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
}
