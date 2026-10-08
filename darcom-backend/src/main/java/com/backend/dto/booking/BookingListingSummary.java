package com.backend.dto.booking;

import com.backend.domain.Listing;

import java.math.BigDecimal;
import java.util.UUID;

/** What was booked — scalars only. Safe wherever booking.listing is fetched, which after the visitor-fetch edit is every read path. */
public record BookingListingSummary(UUID id, String title, String city, BigDecimal pricePerNight, String currency) {
    public static BookingListingSummary from(Listing listing) {
        return new BookingListingSummary(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getPricePerNight(),
                listing.getCurrency()
        );
    }
}
