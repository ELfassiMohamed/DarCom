package com.backend.dto.listing;

import com.backend.domain.enums.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ListingDetailDto(

    UUID id,
    HostSummaryDto host,
    String title,
    String description,
    String city,
    String address,
    BigDecimal pricePerNight,
    String currency,
    boolean mealsIncluded,
    Set<String> activities,
    ListingStatus status,
    List<PhotoDto> photos,
    Instant createdAt,
    Instant updatedAt
) {
    public static ListingDetailDto from(com.backend.domain.Listing listing) {
        return new ListingDetailDto(
                listing.getId(),
                com.backend.dto.listing.HostSummaryDto.from(listing.getHost()),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCity(),
                listing.getAddress(),
                listing.getPricePerNight(),
                listing.getCurrency(),
                listing.isMealsIncluded(),
                listing.getActivities(),
                listing.getStatus(),
                listing.getPhotos().stream()
                        .map(com.backend.dto.listing.PhotoDto::from)
                        .toList(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }
}