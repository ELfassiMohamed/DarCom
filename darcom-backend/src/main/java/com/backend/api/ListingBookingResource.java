package com.backend.api;

import com.backend.api.security.UserSecurityContext;
import com.backend.domain.User;
import com.backend.dto.booking.BookingRequest;
import com.backend.dto.booking.BookingResponse;
import com.backend.service.BookingService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * Create lives here — nested under its listing — because a JAX-RS class has one
 * base path and this endpoint's root is /listings, not /bookings. Spec role:
 * VISITOR (hosts cannot book; the service's own-listing backstop stays for
 * direct callers). Thinner than ListingResource: the service takes scalars, so
 * not even a carrier is built — request fields pass straight through.
 */
@Path("/listings/{listingId}/bookings")
@RolesAllowed("VISITOR")
public class ListingBookingResource {

    private final BookingService bookingService = new BookingService();

    /** POST /listings/{listingId}/bookings — guards (verified/own/inactive/unavailable/dates) all live in the service. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@PathParam("listingId") UUID listingId,
                           @Valid BookingRequest request,
                           @Context ContainerRequestContext ctx) {
        User visitor = ((UserSecurityContext) ctx.getSecurityContext()).getUser();
        return Response.status(Response.Status.CREATED).entity(BookingResponse.from(
                bookingService.create(visitor, listingId, request.getCheckIn(), request.getCheckOut(),
                        request.getGuestsCount(), request.getMessage()))).build();
    }
}
