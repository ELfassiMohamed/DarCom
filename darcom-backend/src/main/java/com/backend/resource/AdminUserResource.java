package com.backend.resource;

import com.backend.dto.common.PageResponse;
import com.backend.dto.user.BlockUserRequest;
import com.backend.dto.user.UserDetailDto;
import com.backend.dto.user.UserDto;
import com.backend.dto.user.UserSummaryDto;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.security.AuthenticatedUser;
import com.backend.security.Secured;
import com.backend.service.AdminUserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/admin/users")
@Secured
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminUserResource {

    private final AdminUserService adminUserService;

    @Inject
    public AdminUserResource(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GET
    public Response list(@QueryParam("role") String role,
                         @QueryParam("verified") String verified,
                         @QueryParam("blocked") String blocked,
                         @QueryParam("search") String search,
                         @QueryParam("page") String page,
                         @QueryParam("size") String size,
                         @QueryParam("sort") String sort) {
        PageResponse<UserSummaryDto> result = adminUserService.list(
                role,
                AdminUserService.parseFlag("verified", verified),
                AdminUserService.parseFlag("blocked", blocked),
                search,
                AdminUserService.parsePage(page),
                AdminUserService.parseSize(size),
                AdminUserService.parseSort(sort));
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") UUID id) {
        UserDetailDto dto = adminUserService.getById(id);
        return Response.ok(dto).build();
    }

    @PATCH
    @Path("/{id}/block")
    public Response block(@PathParam("id") UUID id, @Valid BlockUserRequest request,
                          @Context SecurityContext security) {
        if (request == null) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Request body is required");
        }
        UUID adminId = ((AuthenticatedUser) security.getUserPrincipal()).getId();
        UserDto dto = adminUserService.block(adminId, id, request);
        return Response.ok(dto).build();
    }

    @PATCH
    @Path("/{id}/unblock")
    public Response unblock(@PathParam("id") UUID id) {
        UserDto dto = adminUserService.unblock(id);
        return Response.ok(dto).build();
    }
}
