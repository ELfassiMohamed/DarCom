package com.backend.api;

import com.backend.api.security.UserSecurityContext;
import com.backend.domain.User;
import com.backend.repository.Pageable;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Shared request plumbing for resources: caller resolution (the D1 idiom —
 * request-context getSecurityContext + one cast, never injected-context cast)
 * and defensive pagination (no 400 type exists for bad page/size; coerce).
 * Fourth copy triggered the extraction (was private per-resource helpers).
 * Stateless statics only — no coupling between resources' behavior.
 */
public final class ResourceSupport {

    public static final int MAX_SIZE = 100;

    private ResourceSupport() {
    }

    public static User currentUser(ContainerRequestContext ctx) {
        return ((UserSecurityContext) ctx.getSecurityContext()).getUser();
    }

    public static Pageable normalizePageable(int page, int size) {
        return new Pageable(Math.max(0, page), Math.min(MAX_SIZE, Math.max(1, size)));
    }
}
