package com.backend.service;

import com.backend.domain.Booking;
import com.backend.domain.Listing;
import com.backend.domain.Message;
import com.backend.domain.User;
import com.backend.domain.enums.BookingStatus;
import com.backend.domain.enums.ListingStatus;
import com.backend.domain.enums.UserRole;
import com.backend.exception.ConflictException;
import com.backend.exception.ForbiddenException;
import com.backend.exception.NotFoundException;
import com.backend.repository.BookingRepository;
import com.backend.repository.ListingRepository;
import com.backend.repository.MessageRepository;
import com.backend.repository.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BookingService {

    /** POST /listings/{listingId}/bookings */
    public Booking create(User visitor, UUID listingId, LocalDate checkIn, LocalDate checkOut,
                           int guestsCount, String message) {
        // Plain field checks before any transaction opens (same shape as
        // AuthService.register's role check, Task 04): a missing or zero-length
        // date range never reaches the database. This fulfills Booking's own
        // TODO — "checkOut > checkIn is enforced in service layer" — a class-level
        // rule with no Bean Validation equivalent. Null dates and non-positive
        // guestsCount need no explicit line: JPA AUTO bean validation 400s them
        // at persist via ValidationExceptionMapper; the range rule has no such
        // backstop, so it lives here. Note checkOut.isAfter is strict — a
        // zero-night booking is rejected, while same-day turnover (A's checkout
        // == B's checkin) stays allowed per Task 02's half-open overlap rule.
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new ConflictException("INVALID_DATE_RANGE", "checkOut must be after checkIn");
        }

        return TransactionRunner.call(em -> {
            Listing listing = new ListingRepository(em).findById(listingId)
                    .orElseThrow(() -> new NotFoundException("LISTING_NOT_FOUND", "Listing not found"));

            if (listing.getStatus() != ListingStatus.ACTIVE) {
                throw new ConflictException("LISTING_NOT_ACTIVE", "This listing is not currently accepting bookings");
            }
            if (listing.getHost().getId().equals(visitor.getId())) {
                throw new ConflictException("CANNOT_BOOK_OWN_LISTING", "You cannot book your own listing");
            }

            BookingRepository bookingRepository = new BookingRepository(em);
            boolean unavailable = !bookingRepository
                    .findOverlapping(listing, checkIn, checkOut, List.of(BookingStatus.CONFIRMED), null)
                    .isEmpty();
            if (unavailable) {
                throw new ConflictException("DATES_UNAVAILABLE", "These dates are already booked");
            }

            Booking booking = new Booking();
            booking.setListing(listing);
            booking.setVisitor(visitor);
            booking.setCheckIn(checkIn);
            booking.setCheckOut(checkOut);
            booking.setGuestsCount(guestsCount);
            booking.setStatus(BookingStatus.PENDING);
            bookingRepository.create(booking);

            if (message != null && !message.isBlank()) {
                Message firstMessage = new Message();
                firstMessage.setListing(listing);
                firstMessage.setSender(visitor);
                firstMessage.setReceiver(listing.getHost());
                firstMessage.setBody(message);
                new MessageRepository(em).create(firstMessage);
            }

            return booking;
        });
    }

    /** GET /bookings/{id} — visitor, the listing's host, or an admin; nobody else */
    public Booking getById(User currentUser, UUID bookingId) {
        return TransactionRunner.call(em -> {
            Booking booking = new BookingRepository(em).findByIdWithDetails(bookingId)
                    .orElseThrow(() -> new NotFoundException("BOOKING_NOT_FOUND", "Booking not found"));

            boolean isVisitor = booking.getVisitor().getId().equals(currentUser.getId());
            boolean isHost = booking.getListing().getHost().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

            if (!isVisitor && !isHost && !isAdmin) {
                throw new ForbiddenException("NOT_AUTHORIZED", "You are not authorized to view this booking");
            }
            return booking;
        });
    }

    /** GET /bookings/mine */
    public PagedResult<Booking> findMine(User visitor, Pageable pageable) {
        return TransactionRunner.call(em -> {
            BookingRepository repo = new BookingRepository(em);
            List<Booking> items = repo.findByVisitor(visitor, pageable);
            long totalItems = repo.countByVisitor(visitor);
            return new PagedResult<>(items, totalItems);
        });
    }

    /** GET /bookings/received — status optional */
    public PagedResult<Booking> findReceived(User host, BookingStatus status, Pageable pageable) {
        return TransactionRunner.call(em -> {
            BookingRepository repo = new BookingRepository(em);
            List<Booking> items = repo.findByListingHost(host, status, pageable);
            long totalItems = repo.countByListingHost(host, status);
            return new PagedResult<>(items, totalItems);
        });
    }

    /**
     * PATCH /bookings/{id}/status — host only, and only to CONFIRMED or
     * REJECTED (cancellation is a separate action, §3.1). On CONFIRMED,
     * every other PENDING booking on the same listing whose dates overlap
     * this one gets auto-rejected — the workflow Task 03 explicitly
     * deferred, finally built on top of Task 02's findOverlapping.
     */
    public Booking updateStatus(User host, UUID bookingId, BookingStatus newStatus) {
        return TransactionRunner.call(em -> {
            BookingRepository bookingRepository = new BookingRepository(em);
            Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                    .orElseThrow(() -> new NotFoundException("BOOKING_NOT_FOUND", "Booking not found"));

            if (!booking.getListing().getHost().getId().equals(host.getId())) {
                throw new ForbiddenException("NOT_LISTING_HOST", "Only the listing's host can update a booking's status");
            }
            if (newStatus != BookingStatus.CONFIRMED && newStatus != BookingStatus.REJECTED) {
                throw new ConflictException("INVALID_STATUS_TRANSITION",
                        "A host may only confirm or reject here — cancellation is the visitor's action");
            }
            if (!booking.getStatus().canTransitionTo(newStatus)) {
                throw new ConflictException("INVALID_STATUS_TRANSITION",
                        "Cannot move a booking from " + booking.getStatus() + " to " + newStatus);
            }

            booking.setStatus(newStatus);
            bookingRepository.update(booking);

            if (newStatus == BookingStatus.CONFIRMED) {
                List<Booking> toReject = bookingRepository.findOverlapping(
                        booking.getListing(), booking.getCheckIn(), booking.getCheckOut(),
                        List.of(BookingStatus.PENDING), booking.getId());
                for (Booking other : toReject) {
                    other.setStatus(BookingStatus.REJECTED);
                    bookingRepository.update(other);
                }
            }

            return booking;
        });
    }

    /** PATCH /bookings/{id}/cancel — the visitor who made it, only */
    public Booking cancel(User visitor, UUID bookingId) {
        return TransactionRunner.call(em -> {
            BookingRepository bookingRepository = new BookingRepository(em);
            Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                    .orElseThrow(() -> new NotFoundException("BOOKING_NOT_FOUND", "Booking not found"));

            if (!booking.getVisitor().getId().equals(visitor.getId())) {
                throw new ForbiddenException("NOT_BOOKING_OWNER", "You can only cancel your own bookings");
            }
            if (!booking.getStatus().canTransitionTo(BookingStatus.CANCELLED)) {
                throw new ConflictException("INVALID_STATUS_TRANSITION",
                        "Cannot cancel a booking that is " + booking.getStatus());
            }

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.update(booking);
            return booking;
        });
    }
}
