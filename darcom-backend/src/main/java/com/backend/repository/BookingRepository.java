package com.backend.repository;

import com.backend.domain.Booking;
import com.backend.domain.Listing;
import com.backend.domain.User;
import com.backend.domain.enums.BookingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BookingRepository extends GenericRepository<Booking> {

    public BookingRepository(EntityManager em) {
        super(em, Booking.class);
    }

    /** GET /bookings/mine */
    public List<Booking> findByVisitor(User visitor, Pageable pageable) {
        return em.createQuery(
                "SELECT b FROM Booking b WHERE b.visitor = :visitor ORDER BY b.createdAt DESC",
                Booking.class)
                .setParameter("visitor", visitor)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    /** GET /bookings/received — across all of a host's listings, status optional. */
    public List<Booking> findByListingHost(User host, BookingStatus status, Pageable pageable) {
        String jpql = "SELECT b FROM Booking b WHERE b.listing.host = :host"
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
     * Backs the PATCH /bookings/{id}/status side effect (§7 of the API spec):
     * on CONFIRMED, auto-reject every other PENDING booking on the same
     * listing whose dates overlap. This method only answers "which bookings
     * overlap" — deciding to call it, and rejecting what it returns, is the
     * Service layer's job (§0).
     */
    public List<Booking> findOverlapping(Listing listing, LocalDate checkIn, LocalDate checkOut,
                                          List<BookingStatus> statuses, UUID excludeBookingId) {
        return em.createQuery(
                "SELECT b FROM Booking b " +
                "WHERE b.listing = :listing " +
                "AND b.status IN :statuses " +
                "AND b.id <> :excludeId " +
                "AND b.checkIn < :checkOut " +
                "AND b.checkOut > :checkIn",
                Booking.class)
                .setParameter("listing", listing)
                .setParameter("statuses", statuses)
                .setParameter("excludeId", excludeBookingId)
                .setParameter("checkOut", checkOut)
                .setParameter("checkIn", checkIn)
                .getResultList();
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
