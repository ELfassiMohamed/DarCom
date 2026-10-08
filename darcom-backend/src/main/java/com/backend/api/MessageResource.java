package com.backend.api;

import com.backend.domain.Message;
import com.backend.dto.PagedResponse;
import com.backend.dto.message.ConversationSummary;
import com.backend.repository.Pageable;
import com.backend.service.Conversation;
import com.backend.service.MessageService;
import com.backend.service.PagedResult;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static com.backend.api.ResourceSupport.currentUser;
import static com.backend.api.ResourceSupport.normalizePageable;

@Path("/messages")
@RolesAllowed({"HOST", "VISITOR", "ADMIN"})
public class MessageResource {

    private final MessageService messageService = new MessageService();

    /** GET /messages/conversations — one row per (listing, counterpart), most-recent-first. Items are already DTOs. */
    @GET
    @Path("/conversations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response conversations(@Context ContainerRequestContext ctx,
                                  @QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("size") @DefaultValue("20") int size) {
        Pageable pageable = normalizePageable(page, size);
        PagedResult<Conversation> result = messageService.getConversations(currentUser(ctx), pageable);
        List<ConversationSummary> items = result.items().stream().map(ConversationSummary::from).toList();
        return Response.ok(new PagedResponse<>(items, pageable.page(), pageable.size(), result.totalItems())).build();
    }
}
