package com.backend.repository;

import com.backend.domain.Booking;
import com.backend.domain.Listing;
import com.backend.domain.enums.BookingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ListingRepository extends GenericRepository<Listing> {

    public ListingRepository(EntityManager em) {
        super(em, Listing.class);
    }

    // Public search with dynamic predicates (CriteriaBuilder)
    public List<Listing> search(String city, BigDecimal minPrice, BigDecimal maxPrice,
                                Boolean mealsIncluded, Pageable pageable, String sortField, boolean asc) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Listing> cq = cb.createQuery(Listing.class);
        Root<Listing> root = cq.from(Listing.class);

        Predicate[] predicates = buildSearchPredicates(cb, root, city, minPrice, maxPrice, mealsIncluded);
        cq.where(predicates);

        if (sortField != null) {
            if (asc) {
                cq.orderBy(cb.asc(root.get(sortField)));
            } else {
                cq.orderBy(cb.desc(root.get(sortField)));
            }
        }

        return em.createQuery(cq)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    private Predicate[] buildSearchPredicates(CriteriaBuilder cb, Root<Listing> root, String city,
                                                BigDecimal minPrice, BigDecimal maxPrice, Boolean mealsIncluded) {
        List<Predicate> predicates = new ArrayList<>();

        if (city != null && !city.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
        }
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
        }
        if (mealsIncluded != null) {
            predicates.add(cb.equal(root.get("mealsIncluded"), mealsIncluded));
        }

        return predicates.toArray(new Predicate[0]);
    }

    // Matching count query for pagination envelope
    public long countSearch(String city, BigDecimal minPrice, BigDecimal maxPrice, Boolean mealsIncluded) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Listing> root = cq.from(Listing.class);

        cq.select(cb.count(root)).where(buildSearchPredicates(cb, root, city, minPrice, maxPrice, mealsIncluded));
        return em.createQuery(cq).getSingleResult();
    }

    // Host listings (all statuses: ACTIVE, HIDDEN, REMOVED)
    public List<Listing> findByHost(UUID hostId, Pageable pageable, String sortField, boolean asc) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Listing> cq = cb.createQuery(Listing.class);
        Root<Listing> root = cq.from(Listing.class);

        cq.where(cb.equal(root.get("host").get("id"), hostId));

        if (sortField != null) {
            if (asc) {
                cq.orderBy(cb.asc(root.get(sortField)));
            } else {
                cq.orderBy(cb.desc(root.get(sortField)));
            }
        }

        return em.createQuery(cq)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    public long countByHost(UUID hostId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Listing> root = cq.from(Listing.class);

        cq.select(cb.count(root)).where(cb.equal(root.get("host").get("id"), hostId));
        return em.createQuery(cq).getSingleResult();
    }

    // Check for active bookings preventing soft delete (PENDING, CONFIRMED)
    public boolean hasActiveBookings(UUID listingId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Booking> root = cq.from(Booking.class);

        cq.select(cb.count(root))
                .where(cb.equal(root.get("listing").get("id"), listingId))
                .where(root.get("status").in(Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED)));

        Long count = em.createQuery(cq).getSingleResult();
        return count > 0;
    }
}