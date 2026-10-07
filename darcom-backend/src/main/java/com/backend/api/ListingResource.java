package com.backend.api;

import com.backend.api.security.UserSecurityContext;
import com.backend.domain.Listing;
import com.backend.domain.User;
import com.backend.dto.PagedResponse;
import com.backend.dto.listing.ListingRequest;
import com.backend.dto.listing.ListingResponse;
import com.backend.repository.Pageable;
import com.backend.service.ListingService;
import com.backend.service.PagedResult;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Path("/listings")
public class ListingResource {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ListingService listingService = new ListingService();

    /** POST /listings — HOST only; verified-check lives in the service (EMAIL_NOT_VERIFIED). */
    @POST
    @RolesAllowed("HOST")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Valid ListingRequest request, @Context ContainerRequestContext ctx) {
        Listing created = listingService.create(currentUser(ctx), toListing(request));
        return Response.status(Response.Status.CREATED).entity(ListingResponse.from(created)).build();
    }

    /** GET /listings — public search, ACTIVE only (enforced in the repository, Task 03). Every filter optional. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@QueryParam("city") String city,
                           @QueryParam("minPrice") BigDecimal minPrice,
                           @QueryParam("maxPrice") BigDecimal maxPrice,
                           @QueryParam("mealsIncluded") Boolean mealsIncluded,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size) {
        Pageable pageable = normalizePageable(page, size);
        PagedResult<Listing> result = listingService.search(city, minPrice, maxPrice, mealsIncluded, pageable);
        return Response.ok(toPagedResponse(result, pageable)).build();
    }

    /** GET /listings/mine — HOST only; every status (the host sees their own HIDDEN/REMOVED too). */
    @GET
    @Path("/mine")
    @RolesAllowed("HOST")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findMine(@Context ContainerRequestContext ctx,
                             @QueryParam("page") @DefaultValue("0") int page,
                             @QueryParam("size") @DefaultValue("20") int size) {
        Pageable pageable = normalizePageable(page, size);
        PagedResult<Listing> result = listingService.findMine(currentUser(ctx), pageable);
        return Response.ok(toPagedResponse(result, pageable)).build();
    }

    /** GET /listings/{id} — public, no status filter (Task 03's flagged ambiguity, unchanged: a REMOVED listing still reads back for anyone with the id). */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") UUID id) {
        return Response.ok(ListingResponse.from(listingService.getById(id))).build();
    }

    /** PUT /listings/{id} — HOST + owner (service enforces NOT_OWNER); full replace of the editable fields, same response shape as GET. */
    @PUT
    @Path("/{id}")
    @RolesAllowed("HOST")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") UUID id,
                           @Valid ListingRequest request,
                           @Context ContainerRequestContext ctx) {
        Listing updated = listingService.update(currentUser(ctx), id, toListing(request));
        return Response.ok(ListingResponse.from(updated)).build();
    }

    /** DELETE /listings/{id} — HOST + owner; soft delete (REMOVED), 204 with no body. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("HOST")
    public Response delete(@PathParam("id") UUID id, @Context ContainerRequestContext ctx) {
        listingService.delete(currentUser(ctx), id);
        return Response.noContent().build();
    }

    private User currentUser(ContainerRequestContext ctx) {
        return ((UserSecurityContext) ctx.getSecurityContext()).getUser();
    }

    private Pageable normalizePageable(int page, int size) {
        return new Pageable(Math.max(0, page), Math.min(MAX_SIZE, Math.max(1, size)));
    }

    private PagedResponse<ListingResponse> toPagedResponse(PagedResult<Listing> result, Pageable pageable) {
        List<ListingResponse> items = result.items().stream().map(ListingResponse::from).toList();
        return new PagedResponse<>(items, pageable.page(), pageable.size(), result.totalItems());
    }

    /** Builds the detached "changes" carrier Task 03 §7's update/create expect — never persisted itself. */
    private Listing toListing(ListingRequest request) {
        Listing listing = new Listing();
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setCity(request.getCity());
        listing.setAddress(request.getAddress());
        listing.setPricePerNight(request.getPricePerNight());
        listing.setCurrency(request.getCurrency());
        listing.setMealsIncluded(request.isMealsIncluded());
        listing.setActivities(request.getActivities());
        return listing;
    }
}
