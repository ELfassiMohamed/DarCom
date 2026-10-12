package com.backend.resource;

import com.backend.dto.user.UpdateProfileRequest;
import com.backend.dto.user.UserDto;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.security.AuthenticatedUser;
import com.backend.security.Secured;
import com.backend.service.UserService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/users")
@Secured
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserService userService;

    @Inject
    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    @Path("/me")
    public Response me(@Context SecurityContext security) {
        UserDto dto = userService.getMe(principalId(security));
        return Response.ok(dto).build();
    }

    @PUT
    @Path("/me")
    public Response updateMe(@Valid UpdateProfileRequest request, @Context SecurityContext security) {
        if (request == null) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Request body is required");
        }
        UserDto dto = userService.updateMe(principalId(security), request);
        return Response.ok(dto).build();
    }

    private static java.util.UUID principalId(SecurityContext security) {
        return ((AuthenticatedUser) security.getUserPrincipal()).getId();
    }
}
