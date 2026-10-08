package com.backend.dto.message;

import com.backend.domain.User;

import java.util.UUID;

/** A thread participant or conversation counterpart — id + name only (HostSummary precedent). */
public record ParticipantSummary(UUID id, String fullName) {
    public static ParticipantSummary from(User user) {
        return new ParticipantSummary(user.getId(), user.getFullName());
    }
}
