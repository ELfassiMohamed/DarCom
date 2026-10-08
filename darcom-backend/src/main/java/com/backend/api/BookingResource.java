package com.backend.api;

import com.backend.domain.Booking;
import com.backend.domain.enums.BookingStatus;
import com.backend.dto.PagedResponse;
import com.backend.dto.booking.BookingResponse;
import com.backend.dto.booking.UpdateBookingStatusRequest;
import com.backend.repository.Pageable;
import com.backend.service.BookingService;
import com.backend.service.PagedResult;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

import static com.backend.api.ResourceSupport.currentUser;
import static com.backend.api.ResourceSupport.normalizePageable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/bookings")
@Tag(name = "Bookings", description = "Request, review and cancel stays")
public class BookingResource {

    private final BookingService bookingService = new BookingService();

    /** GET /bookings/{id} — admitted to visitor, listing-host, or admin; the service's three-way check decides (no single role literal expresses it, so the gate stays broad here). */
    @GET
    @Path("/{id}")
    @RolesAllowed({"HOST", "VISITOR", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get one booking (visitor, host or admin)")
    @ApiResponse(responseCode = "200", description = "Full booking")
    @ApiResponse(responseCode = "403", description = "Not a participant")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    public Response getById(@PathParam("id") UUID id, @Context ContainerRequestContext ctx) {
        return Response.ok(BookingResponse.from(bookingService.getById(currentUser(ctx), id))).build();
    }

    /** GET /bookings/mine — the caller's bookings as visitor (spec role: VISITOR). */
    @GET
    @Path("/mine")
    @RolesAllowed("VISITOR")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List the caller's bookings as visitor")
    @ApiResponse(responseCode = "200", description = "Paginated bookings")
    public Response findMine(@Context ContainerRequestContext ctx,
                             @QueryParam("page") @DefaultValue("0") int page,
                             @QueryParam("size") @DefaultValue("20") int size) {
        Pageable pageable = normalizePageable(page, size);
        PagedResult<Booking> result = bookingService.findMine(currentUser(ctx), pageable);
        return Response.ok(toPagedResponse(result, pageable)).build();
    }

    /** GET /bookings/received — the caller's bookings as host (spec role: HOST), status optional. */
    @GET
    @Path("/received")
    @RolesAllowed("HOST")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List incoming booking requests across the caller's listings")
    @ApiResponse(responseCode = "200", description = "Paginated bookings")
    public Response findReceived(@Context ContainerRequestContext ctx,
                                 @QueryParam("status") BookingStatus status,
                                 @QueryParam("page") @DefaultValue("0") int page,
                                 @QueryParam("size") @DefaultValue("20") int size) {
        Pageable pageable = normalizePageable(page, size);
        PagedResult<Booking> result = bookingService.findReceived(currentUser(ctx), status, pageable);
        return Response.ok(toPagedResponse(result, pageable)).build();
    }

    /** PATCH /bookings/{id}/status — spec role HOST, must own the listing (service enforces NOT_LISTING_OWNER); CONFIRMED triggers auto-reject. */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed("HOST")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Confirm or reject a booking (confirm auto-rejects overlapping pendings)")
    @ApiResponse(responseCode = "200", description = "Updated booking")
    @ApiResponse(responseCode = "403", description = "Not the listing host")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @ApiResponse(responseCode = "409", description = "Illegal transition")
    public Response updateStatus(@PathParam("id") UUID id,
                                 @Valid UpdateBookingStatusRequest request,
                                 @Context ContainerRequestContext ctx) {
        Booking updated = bookingService.updateStatus(currentUser(ctx), id,
                request.getStatus(), request.getReason());
        return Response.ok(BookingResponse.from(updated)).build();
    }

    /** PATCH /bookings/{id}/cancel — spec role VISITOR, must own the booking (service enforces NOT_BOOKING_OWNER); 200 with the cancelled booking. */
    @PATCH
    @Path("/{id}/cancel")
    @RolesAllowed("VISITOR")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cancel the caller's own booking")
    @ApiResponse(responseCode = "200", description = "Cancelled booking")
    @ApiResponse(responseCode = "403", description = "Not the booking owner")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @ApiResponse(responseCode = "409", description = "Already terminal")
    public Response cancel(@PathParam("id") UUID id, @Context ContainerRequestContext ctx) {
        return Response.ok(BookingResponse.from(bookingService.cancel(currentUser(ctx), id))).build();
    }

    private PagedResponse<BookingResponse> toPagedResponse(PagedResult<Booking> result, Pageable pageable) {
        List<BookingResponse> items = result.items().stream().map(BookingResponse::from).toList();
        return new PagedResponse<>(items, pageable.page(), pageable.size(), result.totalItems());
    }
}
