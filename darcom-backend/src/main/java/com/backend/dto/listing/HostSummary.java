package com.backend.dto.listing;

import com.backend.domain.User;

import java.util.UUID;

/** The §6 "host summary block", minimal on purpose (§3.2): id + name, no contact data on a public endpoint. */
public record HostSummary(UUID id, String fullName) {
    public static HostSummary from(User user) {
        return new HostSummary(user.getId(), user.getFullName());
    }
}
