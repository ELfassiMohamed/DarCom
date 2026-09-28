package com.backend.service;

import com.backend.domain.Listing;
import com.backend.domain.User;
import com.backend.domain.enums.ListingStatus;
import com.backend.exception.ConflictException;
import com.backend.exception.ForbiddenException;
import com.backend.exception.NotFoundException;
import com.backend.repository.BookingRepository;
import com.backend.repository.ListingRepository;
import com.backend.repository.Pageable;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ListingService {

    /** POST /listings */
    public Listing create(User host, Listing listing) {
        if (!host.isVerified()) {
            throw new ForbiddenException("EMAIL_NOT_VERIFIED", "Verify your email before listing a property");
        }
        listing.setHost(host);
        listing.setStatus(ListingStatus.ACTIVE);

        return TransactionRunner.call(em -> new ListingRepository(em).create(listing));
    }

    /** GET /listings/{id}, and the shape PUT's response reuses */
    public Listing getById(UUID id) {
        return TransactionRunner.call(em -> {
            Listing listing = new ListingRepository(em).findByIdWithDetails(id)
                    .orElseThrow(() -> new NotFoundException("LISTING_NOT_FOUND", "Listing not found"));
            Hibernate.initialize(listing.getActivities());
            return listing;
        });
    }

    /** GET /listings — public search, ACTIVE only, every filter optional */
    public PagedResult<Listing> search(String city, BigDecimal minPrice, BigDecimal maxPrice,
                                        Boolean mealsIncluded, Pageable pageable) {
        return TransactionRunner.call(em -> {
            ListingRepository repo = new ListingRepository(em);
            List<Listing> items = repo.search(city, minPrice, maxPrice, mealsIncluded, pageable);
            long totalItems = repo.countSearch(city, minPrice, maxPrice, mealsIncluded);
            return new PagedResult<>(items, totalItems);
        });
    }

    /** GET /listings/mine — every status, host's own listings only */
    public PagedResult<Listing> findMine(User host, Pageable pageable) {
        return TransactionRunner.call(em -> {
            ListingRepository repo = new ListingRepository(em);
            List<Listing> items = repo.findByHost(host, pageable);
            long totalItems = repo.countByHost(host);
            return new PagedResult<>(items, totalItems);
        });
    }

    /** PUT /listings/{id} — full replace of the editable fields */
    public Listing update(User host, UUID listingId, Listing changes) {
        return TransactionRunner.call(em -> {
            ListingRepository repo = new ListingRepository(em);
            Listing existing = repo.findByIdWithDetails(listingId)
                    .orElseThrow(() -> new NotFoundException("LISTING_NOT_FOUND", "Listing not found"));

            requireOwner(existing, host);

            existing.setTitle(changes.getTitle());
            existing.setDescription(changes.getDescription());
            existing.setCity(changes.getCity());
            existing.setAddress(changes.getAddress());
            existing.setPricePerNight(changes.getPricePerNight());
            existing.setCurrency(changes.getCurrency());
            existing.setMealsIncluded(changes.isMealsIncluded());
            existing.setActivities(changes.getActivities());

            return repo.update(existing);
        });
    }

    /** DELETE /listings/{id} — soft delete: status -> REMOVED, never a hard delete */
    public void delete(User host, UUID listingId) {
        TransactionRunner.execute(em -> {
            ListingRepository listingRepo = new ListingRepository(em);
            Listing listing = listingRepo.findById(listingId)
                    .orElseThrow(() -> new NotFoundException("LISTING_NOT_FOUND", "Listing not found"));

            requireOwner(listing, host);

            if (new BookingRepository(em).existsActiveBookingForListing(listing)) {
                throw new ConflictException("CANNOT_DELETE_WITH_ACTIVE_BOOKINGS",
                        "This listing has pending or confirmed bookings");
            }

            listing.setStatus(ListingStatus.REMOVED);
            listingRepo.update(listing);
        });
    }

    private void requireOwner(Listing listing, User host) {
        if (!listing.getHost().getId().equals(host.getId())) {
            throw new ForbiddenException("NOT_OWNER", "You do not own this listing");
        }
    }
}
