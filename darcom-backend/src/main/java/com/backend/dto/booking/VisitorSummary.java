package com.backend.dto.booking;

import com.backend.domain.User;

import java.util.UUID;

/** Who booked — id + name only. Same minimalism as HostSummary, separate type (different role, may diverge). */
public record VisitorSummary(UUID id, String fullName) {
    public static VisitorSummary from(User user) {
        return new VisitorSummary(user.getId(), user.getFullName());
    }
}
