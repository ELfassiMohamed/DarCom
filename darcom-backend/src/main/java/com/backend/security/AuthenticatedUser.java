package com.backend.security;

import com.backend.domain.enums.UserRole;

import java.security.Principal;
import java.util.UUID;

/** The authenticated caller attached to the request security context. */
public class AuthenticatedUser implements Principal {

    private final UUID id;
    private final String email;
    private final UserRole role;

    public AuthenticatedUser(UUID id, String email, UserRole role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    /** The principal name is the user id string. */
    @Override
    public String getName() {
        return id.toString();
    }
}
