package com.backend.dto.listing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

public record CreateListingRequest(

    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String description,
    @NotBlank @Size(max = 120) String city,
    @Size(max = 255) String address,
    @NotNull @Positive BigDecimal pricePerNight,
    @Size(max = 3) String currency,
    boolean mealsIncluded,
    Set<String> activities
) {
    public static CreateListingRequest from(com.backend.domain.Listing listing) {
        return new CreateListingRequest(
                listing.getTitle(),
                listing.getDescription(),
                listing.getCity(),
                listing.getAddress(),
                listing.getPricePerNight(),
                listing.getCurrency(),
                listing.isMealsIncluded(),
                listing.getActivities()
        );
    }
}