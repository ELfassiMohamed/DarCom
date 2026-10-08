package com.backend.api;

import com.backend.domain.Message;
import com.backend.dto.PagedResponse;
import com.backend.dto.message.MessageResponse;
import com.backend.dto.message.SendMessageRequest;
import com.backend.repository.Pageable;
import com.backend.service.MessageService;
import com.backend.service.PagedResult;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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

@Path("/listings/{listingId}/messages")
@RolesAllowed({"HOST", "VISITOR", "ADMIN"})
@Tag(name = "Messaging", description = "Listing-scoped threads and conversation overview")
public class ListingMessageResource {

    private final MessageService messageService = new MessageService();

    /** POST — sender host requires toUserId (service 409 otherwise); visitor's receiver forced to host. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Send a message about a listing")
    @ApiResponse(responseCode = "201", description = "Message sent")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    @ApiResponse(responseCode = "409", description = "Host omitted toUserId")
    public Response send(@PathParam("listingId") UUID listingId,
                         @Valid SendMessageRequest request,
                         @Context ContainerRequestContext ctx) {
        Message created = messageService.send(listingId, currentUser(ctx),
                request.getToUserId(), request.getBody());
        return Response.status(Response.Status.CREATED).entity(MessageResponse.from(created)).build();
    }

    /** GET thread — withUser required iff the host has several threads here (service 409); chronological envelope. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read one thread, chronological (marks as read)")
    @ApiResponse(responseCode = "200", description = "Paginated thread")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    @ApiResponse(responseCode = "409", description = "withUser required")
    public Response thread(@PathParam("listingId") UUID listingId,
                           @QueryParam("withUser") UUID withUser,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size,
                           @Context ContainerRequestContext ctx) {
        Pageable pageable = normalizePageable(page, size);
        PagedResult<Message> result = messageService.getThread(listingId, currentUser(ctx), withUser, pageable);
        List<MessageResponse> items = result.items().stream().map(MessageResponse::from).toList();
        return Response.ok(new PagedResponse<>(items, pageable.page(), pageable.size(), result.totalItems())).build();
    }
}
