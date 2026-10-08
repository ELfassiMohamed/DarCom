package com.backend.repository;

import com.backend.domain.Booking;
import com.backend.domain.Listing;
import com.backend.domain.User;
import com.backend.domain.enums.BookingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BookingRepository extends GenericRepository<Booking> {

    public BookingRepository(EntityManager em) {
        super(em, Booking.class);
    }

    /** GET /bookings/mine — listing fetch-joined (to-one: pagination-safe) so its title/price stay readable after the transaction closes. */
    public List<Booking> findByVisitor(User visitor, Pageable pageable) {
        return em.createQuery(
                "SELECT b FROM Booking b JOIN FETCH b.listing WHERE b.visitor = :visitor ORDER BY b.createdAt DESC",
                Booking.class)
                .setParameter("visitor", visitor)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    /** GET /bookings/received — across all of a host's listings, status optional. listing + visitor fetch-joined (both to-one: pagination-safe). */
    public List<Booking> findByListingHost(User host, BookingStatus status, Pageable pageable) {
        String jpql = "SELECT b FROM Booking b JOIN FETCH b.listing JOIN FETCH b.visitor WHERE b.listing.host = :host"
                + (status != null ? " AND b.status = :status" : "")
                + " ORDER BY b.createdAt DESC";

        TypedQuery<Booking> query = em.createQuery(jpql, Booking.class).setParameter("host", host);
        if (status != null) query.setParameter("status", status);

        return query.setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    /**
     * Bookings on this listing overlapping [checkIn, checkOut), restricted to
     * the given statuses, excluding one booking by id.
     *
     * Half-open interval overlap: [a1,a2) and [b1,b2) overlap iff a1 < b2 AND
     * a2 > b1. A checkout on the same day as another booking's checkin is NOT
     * an overlap — same-day turnover is allowed.
     *
     * excludeBookingId is nullable: null means "exclude nothing" (the
     * create-time availability check has no booking yet to exclude), a value
     * means the confirm-time auto-reject case. No existing call sites.
     *
     * Backs the PATCH /bookings/{id}/status side effect (§7 of the API spec):
     * on CONFIRMED, auto-reject every other PENDING booking on the same
     * listing whose dates overlap. This method only answers "which bookings
     * overlap" — deciding to call it, and rejecting what it returns, is the
     * Service layer's job (§0).
     */
    public List<Booking> findOverlapping(Listing listing, LocalDate checkIn, LocalDate checkOut,
                                          List<BookingStatus> statuses, UUID excludeBookingId) {
        String jpql = "SELECT b FROM Booking b " +
                "WHERE b.listing = :listing " +
                "AND b.status IN :statuses " +
                (excludeBookingId != null ? "AND b.id <> :excludeId " : "") +
                "AND b.checkIn < :checkOut " +
                "AND b.checkOut > :checkIn";

        TypedQuery<Booking> query = em.createQuery(jpql, Booking.class)
                .setParameter("listing", listing)
                .setParameter("statuses", statuses)
                .setParameter("checkOut", checkOut)
                .setParameter("checkIn", checkIn);
        if (excludeBookingId != null) {
            query.setParameter("excludeId", excludeBookingId);
        }
        return query.getResultList();
    }

    /**
     * Eagerly loads listing and visitor so both stay readable after the
     * transaction closes — same shape as ListingRepository.findByIdWithDetails
     * (Task 03 §6.1). No DISTINCT needed: two to-one joins never duplicate rows
     * (that guard was for a fetch-joined collection).
     */
    public Optional<Booking> findByIdWithDetails(UUID id) {
        return em.createQuery(
                "SELECT b FROM Booking b JOIN FETCH b.listing JOIN FETCH b.visitor WHERE b.id = :id",
                Booking.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    /** Total count for GET /bookings/mine's pagination envelope (countByHost-style gap, Task 03). */
    public long countByVisitor(User visitor) {
        return em.createQuery("SELECT COUNT(b) FROM Booking b WHERE b.visitor = :visitor", Long.class)
                .setParameter("visitor", visitor)
                .getSingleResult();
    }

    /** Total count for GET /bookings/received's pagination envelope, status optional. */
    public long countByListingHost(User host, BookingStatus status) {
        String jpql = "SELECT COUNT(b) FROM Booking b WHERE b.listing.host = :host"
                + (status != null ? " AND b.status = :status" : "");
        TypedQuery<Long> query = em.createQuery(jpql, Long.class).setParameter("host", host);
        if (status != null) query.setParameter("status", status);
        return query.getSingleResult();
    }

    /** DELETE /listings/{id} guard: 409 CANNOT_DELETE_WITH_ACTIVE_BOOKINGS. */
    public boolean existsActiveBookingForListing(Listing listing) {
        Long count = em.createQuery(
                "SELECT COUNT(b) FROM Booking b WHERE b.listing = :listing AND b.status IN :statuses",
                Long.class)
                .setParameter("listing", listing)
                .setParameter("statuses", List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .getSingleResult();
        return count > 0;
    }
}
