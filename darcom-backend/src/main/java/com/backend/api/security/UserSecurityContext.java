package com.backend.api.security;

import com.backend.domain.User;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Carries the User the filter already resolved through the rest of the
 * request. A resource method gets it back in one cast, no second lookup:
 * ((UserSecurityContext) securityContext).getUser()
 */
public class UserSecurityContext implements SecurityContext {

    private final User user;
    private final boolean secure;

    public UserSecurityContext(User user, boolean secure) {
        this.user = user;
        this.secure = secure;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Principal getUserPrincipal() {
        return () -> user.getId().toString();
    }

    @Override
    public boolean isUserInRole(String role) {
        return user.getRole().name().equals(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }
}
