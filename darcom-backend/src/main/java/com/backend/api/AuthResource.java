package com.backend.api;

import com.backend.domain.User;
import com.backend.dto.auth.AuthResponse;
import com.backend.dto.auth.LoginRequest;
import com.backend.dto.auth.RefreshTokenRequest;
import com.backend.dto.auth.RegisterRequest;
import com.backend.dto.auth.RegisterResponse;
import com.backend.service.AuthResult;
import com.backend.service.AuthService;
import com.backend.service.RefreshResult;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthResource {

    private final AuthService authService = new AuthService();

    @POST
    @Path("/register")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@Valid RegisterRequest request) {
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setFullName(request.getFullName());
        newUser.setPhone(request.getPhone());
        newUser.setRole(request.getRole());

        User created = authService.register(newUser, request.getPassword());
        return Response.status(Response.Status.CREATED).entity(RegisterResponse.from(created)).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Valid LoginRequest request) {
        AuthResult result = authService.login(request.getEmail(), request.getPassword());
        return Response.ok(AuthResponse.from(result)).build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh(@Valid RefreshTokenRequest request) {
        RefreshResult result = authService.refresh(request.getRefreshToken());
        return Response.ok(result).build();
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(@Valid RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return Response.noContent().build();
    }
}
