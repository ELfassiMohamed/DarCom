package com.backend.dto.booking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * POST body for /listings/{listingId}/bookings. listingId comes from the URL,
 * not the body. Dates are ISO "yyyy-MM-dd" (JacksonConfig's JavaTimeModule).
 *
 * The range rule lives here (DTO `@AssertTrue` → 400 VALIDATION_ERROR,
 * spec-literal) AND in the service (`INVALID_DATE_RANGE` backstop for direct
 * callers) — defense in depth across the two different caller kinds, not a
 * split rule: HTTP callers always hit this one first.
 */
public class BookingRequest {

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    /** Missing key deserializes to 0, which fails @Positive → 400. Mirrors the entity's @Positive exactly. */
    @Positive
    private int guestsCount;

    /** Optional first message to the host (TEXT, unbounded like description). Blank/absent = no message row. */
    private String message;

    public BookingRequest() {
    }

    @AssertTrue(message = "checkOut must be after checkIn")
    @JsonIgnore
    public boolean isValidRange() {
        if (checkIn == null || checkOut == null) {
            return true; // @NotNull owns the null case
        }
        return checkOut.isAfter(checkIn);
    }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public int getGuestsCount() { return guestsCount; }
    public void setGuestsCount(int guestsCount) { this.guestsCount = guestsCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
