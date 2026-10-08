package com.backend.dto.booking;

import com.backend.domain.Booking;
import com.backend.domain.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Full booking shape for GETs, status/cancel responses, and create. totalPrice
 * is computed read-time (nights × pricePerNight, spec §14.1 per-night model),
 * stored nowhere. rejectionReason is present only when set (nullable column).
 *
 * Safe post-transaction ONLY under the fetch contract (listing+visitor fetched
 * on every read path). Defensive by field list: no host block, no photos, no
 * activities exist to touch — listing.host/photos are unreachable by
 * construction, exactly like passwordHash in UserResponse.
 */
public record BookingResponse(
        UUID id,
        BookingListingSummary listing,
        VisitorSummary visitor,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestsCount,
        BigDecimal totalPrice,
        BookingStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookingResponse from(Booking booking) {
        long nights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
        BigDecimal totalPrice = booking.getListing().getPricePerNight()
                .multiply(BigDecimal.valueOf(Math.max(nights, 0)));
        return new BookingResponse(
                booking.getId(),
                BookingListingSummary.from(booking.getListing()),
                VisitorSummary.from(booking.getVisitor()),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getGuestsCount(),
                totalPrice,
                booking.getStatus(),
                booking.getRejectionReason(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
