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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/auth")
@Tag(name = "Auth", description = "Registration, login, token management")
public class AuthResource {

    private final AuthService authService = new AuthService();

    @POST
    @Path("/register")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Register a new HOST or VISITOR account")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "Email already exists")
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
    @Operation(summary = "Log in with email and password")
    @ApiResponse(responseCode = "200", description = "Tokens issued")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "403", description = "Account blocked")
    public Response login(@Valid LoginRequest request) {
        AuthResult result = authService.login(request.getEmail(), request.getPassword());
        return Response.ok(AuthResponse.from(result)).build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Issue a fresh access token from a refresh token")
    @ApiResponse(responseCode = "200", description = "Access token issued")
    @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired")
    public Response refresh(@Valid RefreshTokenRequest request) {
        RefreshResult result = authService.refresh(request.getRefreshToken());
        return Response.ok(result).build();
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke a refresh token")
    @ApiResponse(responseCode = "204", description = "Token revoked (idempotent)")
    public Response logout(@Valid RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return Response.noContent().build();
    }
}
