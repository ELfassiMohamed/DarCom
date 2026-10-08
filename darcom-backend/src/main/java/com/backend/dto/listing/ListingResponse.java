package com.backend.dto.listing;

import com.backend.domain.Listing;
import com.backend.domain.enums.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Full §6 GET/PUT shape: scalars + host summary + photos. Safe to call outside
 * any transaction ONLY because of the §§4–5 contract (host fetched, collections
 * initialized). Defensive copies of the collections so DTO mutation can never
 * reach back into a managed entity.
 */
public record ListingResponse(
        UUID id,
        String title,
        String description,
        String city,
        String address,
        BigDecimal pricePerNight,
        String currency,
        boolean mealsIncluded,
        Set<String> activities,
        ListingStatus status,
        HostSummary host,
        List<PhotoResponse> photos,
        Instant createdAt,
        Instant updatedAt
) {
    public static ListingResponse from(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCity(),
                listing.getAddress(),
                listing.getPricePerNight(),
                listing.getCurrency(),
                listing.isMealsIncluded(),
                new LinkedHashSet<>(listing.getActivities()),
                listing.getStatus(),
                HostSummary.from(listing.getHost()),
                listing.getPhotos().stream().map(PhotoResponse::from).toList(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }
}
