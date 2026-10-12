package com.backend.resource;

import com.backend.dto.auth.ForgotPasswordRequest;
import com.backend.dto.auth.LoginRequest;
import com.backend.dto.auth.LoginResponse;
import com.backend.dto.auth.LogoutRequest;
import com.backend.dto.auth.RefreshRequest;
import com.backend.dto.auth.RefreshResponse;
import com.backend.dto.auth.RegisterRequest;
import com.backend.dto.auth.RegisterResponse;
import com.backend.dto.auth.ResendVerificationRequest;
import com.backend.dto.auth.ResetPasswordRequest;
import com.backend.dto.auth.VerifyEmailRequest;
import com.backend.dto.user.UserDto;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.security.AuthenticatedUser;
import com.backend.security.Secured;
import com.backend.service.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;

    @Inject
    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest request) {
        requireBody(request);
        RegisterResponse response = authService.register(request);
        return Response.status(201).entity(response).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        requireBody(request);
        LoginResponse response = authService.login(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@Valid RefreshRequest request) {
        requireBody(request);
        RefreshResponse response = authService.refresh(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/logout")
    @Secured
    public Response logout(@Valid LogoutRequest request, @Context SecurityContext security) {
        requireBody(request);
        AuthenticatedUser principal = (AuthenticatedUser) security.getUserPrincipal();
        authService.logout(principal.getId(), request);
        return Response.noContent().build();
    }

    @POST
    @Path("/verify-email")
    public Response verifyEmail(@Valid VerifyEmailRequest request) {
        requireBody(request);
        UserDto response = authService.verifyEmail(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/resend-verification")
    public Response resendVerification(@Valid ResendVerificationRequest request) {
        requireBody(request);
        authService.resendVerification(request);
        return Response.noContent().build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(@Valid ForgotPasswordRequest request) {
        requireBody(request);
        authService.forgotPassword(request);
        return Response.noContent().build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        requireBody(request);
        authService.resetPassword(request);
        return Response.noContent().build();
    }

    private static void requireBody(Object body) {
        if (body == null) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Request body is required");
        }
    }
}
