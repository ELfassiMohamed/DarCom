package com.backend.api;

import com.backend.domain.User;
import com.backend.dto.auth.UserResponse;
import com.backend.dto.user.UpdateProfileRequest;
import com.backend.service.UserService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static com.backend.api.ResourceSupport.currentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/users/me")
@RolesAllowed({"HOST", "VISITOR", "ADMIN"})
@Tag(name = "Profile", description = "Read and update your own profile")
public class UserResource {

    private final UserService userService = new UserService();

    /** GET /users/me — the caller as UserResponse. No DB round trip: scalars ride the SecurityContext user. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get your own profile")
    @ApiResponse(responseCode = "200", description = "Your profile")
    public Response getMe(@Context ContainerRequestContext ctx) {
        return Response.ok(UserResponse.from(currentUser(ctx))).build();
    }

    /** PUT /users/me — fullName + phone only. */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update your name and phone")
    @ApiResponse(responseCode = "200", description = "Updated profile")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public Response updateMe(@Valid UpdateProfileRequest request, @Context ContainerRequestContext ctx) {
        User updated = userService.updateProfile(currentUser(ctx), request.getFullName(), request.getPhone());
        return Response.ok(UserResponse.from(updated)).build();
    }
}
