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

@Path("/listings/{listingId}/messages")
@RolesAllowed({"HOST", "VISITOR", "ADMIN"})
public class ListingMessageResource {

    private final MessageService messageService = new MessageService();

    /** POST — sender host requires toUserId (service 409 otherwise); visitor's receiver forced to host. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
