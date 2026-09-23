package com.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import com.backend.domain.enums.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @NotBlank
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "address", length = 255)
    private String address;

    @NotNull
    @PositiveOrZero
    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @NotBlank
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "MAD";

    @Column(name = "meals_included", nullable = false)
    private boolean mealsIncluded = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "listing_activities",
            joinColumns = @JoinColumn(name = "listing_id")
    )
    @Column(name = "activity", nullable = false, length = 120)
    private Set<String> activities = new LinkedHashSet<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ListingStatus status = ListingStatus.ACTIVE;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Photo> photos = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Listing() {
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addPhoto(Photo photo) {
        photos.add(photo);
        photo.setListing(this);
    }

    public void removePhoto(Photo photo) {
        photos.remove(photo);
        photo.setListing(null);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }

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

    public void setActivities(Set<String> activities) {
        this.activities.clear();
        if (activities != null) this.activities.addAll(activities);
    }

    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }

    public List<Photo> getPhotos() { return photos; }

    public void setPhotos(List<Photo> photos) {
        this.photos.clear();
        if (photos != null) {
            for (Photo photo : photos) {
                addPhoto(photo);
            }
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Listing other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public final int hashCode() {
        return Listing.class.hashCode();
    }

    @Override
    public String toString() {
        return "Listing{id=" + id + ", title='" + title + "'}";
    }
}
