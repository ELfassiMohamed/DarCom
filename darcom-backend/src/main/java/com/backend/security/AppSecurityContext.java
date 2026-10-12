package com.backend.security;

import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

/** Security context carrying the current-DB-role {@link AuthenticatedUser}. */
public class AppSecurityContext implements SecurityContext {

    private final AuthenticatedUser user;
    private final SecurityContext delegate;

    public AppSecurityContext(AuthenticatedUser user, SecurityContext delegate) {
        this.user = user;
        this.delegate = delegate;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        return user.getRole().name().equals(role);
    }

    @Override
    public boolean isSecure() {
        return delegate != null && delegate.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }
}
