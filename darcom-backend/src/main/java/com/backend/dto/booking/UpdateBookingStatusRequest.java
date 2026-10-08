package com.backend.dto.booking;

import jakarta.validation.constraints.NotNull;

import com.backend.domain.enums.BookingStatus;

/**
 * PATCH /bookings/{id}/status body. Accepts any non-null status — including
 * CANCELLED and PENDING — and lets the service 409 what the host may not
 * request (two-layer defense, same pattern as ADMIN through RegisterRequest).
 * reason accompanies REJECTED (spec §7) and is stored on the booking; ignored
 * for CONFIRMED.
 */
public class UpdateBookingStatusRequest {

    @NotNull
    private BookingStatus status;

    private String reason;

    public UpdateBookingStatusRequest() {
    }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
