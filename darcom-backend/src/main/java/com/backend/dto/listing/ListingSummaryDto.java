package com.backend.dto.listing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ListingSummaryDto(

    UUID id,
    String title,
    String city,
    BigDecimal pricePerNight,
    String currency,
    String mainPhotoUrl,
    String hostName
) {
    public static ListingSummaryDto from(com.backend.domain.Listing listing) {
        String mainPhotoUrl = listing.getPhotos().stream()
                .findFirst()
                .map(com.backend.domain.Photo::getUrl)
                .orElse(null);
        return new ListingSummaryDto(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getPricePerNight(),
                listing.getCurrency(),
                mainPhotoUrl,
                null
        );
    }
}