package com.backend.dto.listing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Request bean for POST (create) and PUT (full replace) — same shape both ways,
 * since PUT replaces all editable fields (Task 03 §7). host/status/id are never
 * accepted: the service sets host+status, the URL carries the id.
 */
public class ListingRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotBlank
    @Size(max = 120)
    private String city;

    @Size(max = 255)
    private String address;

    @NotNull
    @PositiveOrZero
    private BigDecimal pricePerNight;

    /** Required (§3.6) — a missing value would overwrite the entity's "MAD" default with null. */
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    private boolean mealsIncluded = false;

    /** Element constraint: each activity ≤120 mirrors listing_activities.activity (Task 01). Null stays empty — the resource passes it straight to setActivities, whose clear-then-addAll tolerates null. */
    private Set<@Size(max = 120) String> activities;

    public ListingRequest() {
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isMealsIncluded() { return mealsIncluded; }
    public void setMealsIncluded(boolean mealsIncluded) { this.mealsIncluded = mealsIncluded; }

    public Set<String> getActivities() { return activities; }
    public void setActivities(Set<String> activities) { this.activities = activities; }
}
