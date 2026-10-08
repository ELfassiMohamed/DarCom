package com.backend.dto.admin;

import com.backend.dto.auth.UserResponse;
import com.backend.service.UserDetail;

/**
 * GET /admin/users/{id} shape: the user plus the two counts the spec's detail
 * view requires. Composes UserResponse rather than duplicating its fields.
 */
public record AdminUserResponse(UserResponse user, long listingCount, long reportCount) {
    public static AdminUserResponse from(UserDetail detail) {
        return new AdminUserResponse(UserResponse.from(detail.user()),
                detail.listingCount(), detail.reportCount());
    }
}
