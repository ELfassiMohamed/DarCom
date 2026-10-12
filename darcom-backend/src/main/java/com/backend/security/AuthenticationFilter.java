package com.backend.security;

import com.backend.domain.User;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.persistence.Tx;
import com.backend.repository.UserRepository;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Rejects unauthenticated requests (401) before role checks run.
 * Loads the user by primary key on each request so a block takes effect immediately,
 * and uses the current DB role for the security context.
 */
@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private final JwtService jwtService;

    @Inject
    public AuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String header = request.getHeaderString("Authorization");
        if (header == null || header.length() < 8 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ApiException(401, ErrorCode.UNAUTHENTICATED, "Missing or invalid Authorization header");
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            throw new ApiException(401, ErrorCode.UNAUTHENTICATED, "Missing or invalid Authorization header");
        }

        JwtService.AccessClaims claims;
        try {
            claims = jwtService.parseAccess(token);
        } catch (InvalidTokenException e) {
            throw new ApiException(401, ErrorCode.UNAUTHENTICATED, "Invalid or expired token");
        }

        UUID userId = claims.userId();
        Optional<User> found = Tx.inRead(em -> new UserRepository(em).findById(userId));
        User user = found.orElseThrow(
                () -> new ApiException(401, ErrorCode.UNAUTHENTICATED, "User no longer exists"));
        if (user.isBlocked()) {
            throw new ApiException(403, ErrorCode.ACCOUNT_BLOCKED, "Account is blocked");
        }

        SecurityContext original = request.getSecurityContext();
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
        request.setSecurityContext(new AppSecurityContext(principal, original));
    }
}
