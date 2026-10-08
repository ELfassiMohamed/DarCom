package com.backend.dto.listing;

import com.backend.domain.Listing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spec §1/§6 list shape: summary fields only, plus the two denormalized
 * display fields the spec example shows (mainPhotoUrl, hostName). mainPhotoUrl
 * is the first photo by sortOrder, null when the listing has none. Safe
 * wherever host is fetched and photos initialized — true for all list paths.
 */
public record ListingSummaryResponse(
        UUID id,
        String title,
        String city,
        BigDecimal pricePerNight,
        String currency,
        String mainPhotoUrl,
        String hostName
) {
    public static ListingSummaryResponse from(Listing listing) {
        String mainPhotoUrl = listing.getPhotos().isEmpty()
                ? null
                : listing.getPhotos().get(0).getUrl();
        return new ListingSummaryResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getPricePerNight(),
                listing.getCurrency(),
                mainPhotoUrl,
                listing.getHost().getFullName()
        );
    }
}
