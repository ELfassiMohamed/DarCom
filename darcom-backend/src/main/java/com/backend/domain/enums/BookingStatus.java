package com.backend.domain.enums;

import java.util.Set;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED;

    /** Terminal states allow no further transitions. */
    public boolean isTerminal() {
        return this == REJECTED || this == CANCELLED;
    }

    /**
     * Legal transitions per API-Specification.md §7.
     * PENDING   -> CONFIRMED | REJECTED | CANCELLED
     * CONFIRMED -> CANCELLED
     * REJECTED, CANCELLED -> (none)
     */
    public boolean canTransitionTo(BookingStatus target) {
        if (target == null || target == this) return false;
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, REJECTED, CANCELLED).contains(target);
            case CONFIRMED -> target == CANCELLED;
            case REJECTED, CANCELLED -> false;
        };
    }
}
