package com.backend.api;

import com.backend.domain.User;
import com.backend.domain.enums.UserRole;
import com.backend.dto.PagedResponse;
import com.backend.dto.admin.AdminUserResponse;
import com.backend.dto.admin.BlockUserRequest;
import com.backend.dto.auth.UserResponse;
import com.backend.repository.Pageable;
import com.backend.service.PagedResult;
import com.backend.service.UserDetail;
import com.backend.service.UserService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

import static com.backend.api.ResourceSupport.normalizePageable;

@Path("/admin/users")
@RolesAllowed("ADMIN")
public class AdminUserResource {

    private final UserService userService = new UserService();

    /** GET /admin/users — all four filters optional. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@QueryParam("role") UserRole role,
                         @QueryParam("verified") Boolean verified,
                         @QueryParam("blocked") Boolean blocked,
                         @QueryParam("search") String searchText,
                         @QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size) {
        Pageable pageable = normalizePageable(page, size);
        PagedResult<User> result = userService.getAll(role, verified, blocked, searchText, pageable);
        List<UserResponse> items = result.items().stream().map(UserResponse::from).toList();
        return Response.ok(new PagedResponse<>(items, pageable.page(), pageable.size(), result.totalItems())).build();
    }

    /** GET /admin/users/{id} — user plus listing/report counts. */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") UUID id) {
        UserDetail detail = userService.getDetail(id);
        return Response.ok(AdminUserResponse.from(detail)).build();
    }

    /** PATCH /admin/users/{id}/block — blocks and hides ACTIVE listings, same tx. Reason accepted, not persisted (no reader exists). */
    @PATCH
    @Path("/{id}/block")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response block(@PathParam("id") UUID id, BlockUserRequest request) {
        User blocked = userService.block(id);
        return Response.ok(UserResponse.from(blocked)).build();
    }

    /** PATCH /admin/users/{id}/unblock — clears blocked only; hidden listings stay hidden (spec explicit). */
    @PATCH
    @Path("/{id}/unblock")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unblock(@PathParam("id") UUID id) {
        return Response.ok(UserResponse.from(userService.unblock(id))).build();
    }
}
