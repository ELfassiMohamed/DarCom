package com.backend.resource;

import com.backend.dto.listing.CreateListingRequest;
import com.backend.dto.listing.ListingDetailDto;
import com.backend.dto.listing.PhotoDto;
import com.backend.security.AuthenticatedUser;
import com.backend.security.Secured;
import com.backend.service.ListingService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.UUID;

@Path("/listings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ListingResource {

    private final ListingService listingService;

    @Inject
    public ListingResource(ListingService listingService) {
        this.listingService = listingService;
    }

    // 1. POST /listings (Create)
    @POST
    @Secured
    @RolesAllowed("HOST")
    public Response create(@Valid CreateListingRequest req, @Context SecurityContext sec) {
        UUID hostId = principalId(sec);
        ListingDetailDto dto = listingService.create(hostId, req);
        return Response.status(201).entity(dto).build();
    }

    // 2. GET /listings (Public Search)
    @GET
    public Response list(
            @QueryParam("city") String city,
            @QueryParam("minPrice") String minPrice,
            @QueryParam("maxPrice") String maxPrice,
            @QueryParam("mealsIncluded") String mealsIncluded,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("sort") String sort
    ) {
        BigDecimal min = minPrice != null && !minPrice.isBlank() ? new BigDecimal(minPrice) : null;
        BigDecimal max = maxPrice != null && !maxPrice.isBlank() ? new BigDecimal(maxPrice) : null;
        Boolean meals = "true".equalsIgnoreCase(mealsIncluded) ? true : ("false".equalsIgnoreCase(mealsIncluded) ? false : null);
        
        int p = page != null ? page : 0;
        int s = size != null ? size : 20;

        return listingService.listPublic(city, min, max, meals, p, s, sort);
    }

    // 3. GET /listings/mine (Host's own listings - defined BEFORE /{id} to avoid path collision)
    @GET
    @Path("/mine")
    @Secured
    @RolesAllowed("HOST")
    public Response listMine(@Context SecurityContext sec,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("sort") String sort) {
        UUID hostId = principalId(sec);
        int p = page != null ? page : 0;
        int s = size != null ? size : 20;
        return listingService.listMine(hostId, p, s, sort);
    }

    // 4. GET /listings/{id} (Public Detail)
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") UUID id) {
        ListingDetailDto dto = listingService.getById(id);
        return Response.ok(dto).build();
    }

    // 5. PUT /listings/{id} (Full Replace)
    @PUT
    @Path("/{id}")
    @Secured
    @RolesAllowed("HOST")
    public Response update(@PathParam("id") UUID id, 
           @Valid CreateListingRequest req, 
           @Context SecurityContext sec) {
        UUID hostId = principalId(sec);
        ListingDetailDto dto = listingService.update(id, hostId, req);
        return Response.ok(dto).build();
    }

    // 6. DELETE /listings/{id} (Soft Delete)
    @DELETE
    @Path("/{id}")
    @Secured
    @RolesAllowed("HOST")
    public Response delete(@PathParam("id") UUID id, @Context SecurityContext sec) {
        UUID hostId = principalId(sec);
        return listingService.delete(id, hostId);
    }

    // 7. POST /listings/{id}/photos (Photo Upload)
    @POST
    @Path("/{id}/photos")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Secured
    @RolesAllowed("HOST")
    public Response uploadPhoto(
            @PathParam("id") UUID id,
            @Context SecurityContext sec,
            @FormDataParam("file") InputStream fileStream,
            @FormDataParam("file") FormDataContentDisposition fileMetaData
    ) {
        UUID hostId = principalId(sec);
        PhotoDto dto = listingService.addPhoto(id, hostId, fileStream, fileMetaData);
        return Response.status(201).entity(dto).build();
    }

    // 8. DELETE /listings/{id}/photos/{photoId}
    @DELETE
    @Path("/{id}/photos/{photoId}")
    @Secured
    @RolesAllowed("HOST")
    public Response deletePhoto(
            @PathParam("id") UUID id,
            @PathParam("photoId") UUID photoId,
            @Context SecurityContext sec
    ) {
        UUID hostId = principalId(sec);
        return listingService.deletePhoto(id, photoId, hostId);
    }

    private static UUID principalId(SecurityContext security) {
        return ((AuthenticatedUser) security.getUserPrincipal()).getId();
    }
}