package com.backend.resource;

import com.backend.dto.auth.LoginRequest;
import com.backend.dto.auth.LoginResponse;
import com.backend.dto.auth.RegisterRequest;
import com.backend.dto.auth.RegisterResponse;
import com.backend.dto.user.UserDto;
import com.backend.security.AuthenticatedUser;
import com.backend.domain.enums.UserRole;
import com.backend.service.AuthService;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthResource")
class AuthResourceTest {

    private AuthService authService;
    private AuthResource resource;

    private AuthenticatedUser hostPrincipal() {
        return new AuthenticatedUser(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                "host@test.com",
                UserRole.HOST
        );
    }

    private SecurityContext securityContext(AuthenticatedUser user) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return user;
            }

            @Override
            public boolean isUserInRole(String role) {
                return user.getRole().name().equals(role);
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        };
    }

    private RegisterRequest validRegisterRequest() {
        return new RegisterRequest("host@test.com", "Str0ng!Pass", "Test Host", "HOST");
    }

    private RegisterResponse validRegisterResponse() {
        return new RegisterResponse(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                "host@test.com", "Test Host", "HOST", false,
                Instant.parse("2026-10-12T13:00:00Z")
        );
    }

    private LoginRequest validLoginRequest() {
        return new LoginRequest("host@test.com", "Str0ng!Pass");
    }

    private LoginResponse validLoginResponse() {
        return new LoginResponse(
                "access-token-123", "refresh-token-456", 900L,
                new com.backend.dto.auth.LoginUserDto(
                        UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                        "host@test.com", "HOST", false
                )
        );
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 when registering valid HOST")
        void shouldReturn201_whenRegisteringValidHost() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            when(authService.register(any(RegisterRequest.class))).thenReturn(validRegisterResponse());

            Response response = resource.register(validRegisterRequest());

            assertEquals(201, response.getStatus());
            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("should return 201 when registering valid VISITOR")
        void shouldReturn201_whenRegisteringValidVisitor() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            RegisterRequest visitorReq = new RegisterRequest(
                    "visitor@test.com", "Str0ng!Pass", "Test Visitor", "VISITOR");
            when(authService.register(any(RegisterRequest.class))).thenReturn(validRegisterResponse());

            Response response = resource.register(visitorReq);

            assertEquals(201, response.getStatus());
        }

        @Test
        @DisplayName("should throw EMAIL_ALREADY_EXISTS when email already exists")
        void shouldThrowEmailAlreadyExists_whenEmailAlreadyExists() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            409, "EMAIL_ALREADY_EXISTS", "Email is already registered"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.register(validRegisterRequest())
            );
            assertEquals(409, ex.getStatus());
        }

        @Test
        @DisplayName("should throw VALIDATION_ERROR when role is ADMIN")
        void shouldThrowValidationError_whenRoleIsAdmin() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            RegisterRequest adminReq = new RegisterRequest(
                    "admin@test.com", "Str0ng!Pass", "Test Admin", "ADMIN");
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            400, "VALIDATION_ERROR", "Invalid role"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.register(adminReq)
            );
            assertEquals(400, ex.getStatus());
        }

        @Test
        @DisplayName("should throw VALIDATION_ERROR when missing fields")
        void shouldThrowValidationError_whenMissingFields() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.register(null)
            );
            assertEquals(400, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 with tokens when credentials valid")
        void shouldReturn200_whenCredentialsValid() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            when(authService.login(any(LoginRequest.class))).thenReturn(validLoginResponse());

            Response response = resource.login(validLoginRequest());

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS when wrong password")
        void shouldThrowInvalidCredentials_whenWrongPassword() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            401, "INVALID_CREDENTIALS", "Invalid email or password"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.login(validLoginRequest())
            );
            assertEquals(401, ex.getStatus());
        }

        @Test
        @DisplayName("should throw ACCOUNT_BLOCKED when account blocked")
        void shouldThrowAccountBlocked_whenAccountBlocked() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            403, "ACCOUNT_BLOCKED", "Account is blocked"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.login(validLoginRequest())
            );
            assertEquals(403, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("should return 200 with new access token")
        void shouldReturn200_whenRefreshTokenValid() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            com.backend.dto.auth.RefreshResponse refreshResp =
                    new com.backend.dto.auth.RefreshResponse("new-access-token", 900L);
            when(authService.refresh(any(com.backend.dto.auth.RefreshRequest.class)))
                    .thenReturn(refreshResp);

            com.backend.dto.auth.RefreshRequest refreshReq =
                    new com.backend.dto.auth.RefreshRequest("refresh-token-456");

            Response response = resource.refresh(refreshReq);

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw INVALID_OR_EXPIRED_REFRESH_TOKEN when token expired")
        void shouldThrowInvalidRefreshToken_whenTokenExpired() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            when(authService.refresh(any(com.backend.dto.auth.RefreshRequest.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            401, "INVALID_OR_EXPIRED_REFRESH_TOKEN", "Invalid or expired refresh token"));

            com.backend.dto.auth.RefreshRequest refreshReq =
                    new com.backend.dto.auth.RefreshRequest("expired-token");

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.refresh(refreshReq)
            );
            assertEquals(401, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("should return 204 when authenticated")
        void shouldReturn204_whenAuthenticated() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            doNothing().when(authService).logout(any(UUID.class), any(com.backend.dto.auth.LogoutRequest.class));

            com.backend.dto.auth.LogoutRequest logoutReq =
                    new com.backend.dto.auth.LogoutRequest("refresh-token-456");

            Response response = resource.logout(logoutReq, securityContext(hostPrincipal()));

            assertEquals(204, response.getStatus());
        }

        @Test
        @DisplayName("should throw NPE when not authenticated")
        void shouldThrowNpe_whenNotAuthenticated() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            com.backend.dto.auth.LogoutRequest logoutReq =
                    new com.backend.dto.auth.LogoutRequest("refresh-token-456");

            assertThrows(NullPointerException.class,
                    () -> resource.logout(logoutReq, securityContext(null)));
        }
    }

    @Nested
    @DisplayName("POST /auth/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("should return 200 when token valid")
        void shouldReturn200_whenTokenValid() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            UserDto userDto = new UserDto(
                    UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                    "host@test.com", "Test Host", null, "HOST", true, false,
                    Instant.parse("2026-10-12T13:00:00Z")
            );
            when(authService.verifyEmail(any(com.backend.dto.auth.VerifyEmailRequest.class)))
                    .thenReturn(userDto);

            com.backend.dto.auth.VerifyEmailRequest verifyReq =
                    new com.backend.dto.auth.VerifyEmailRequest("valid-token");

            Response response = resource.verifyEmail(verifyReq);

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw INVALID_OR_EXPIRED_VERIFICATION_TOKEN when token expired")
        void shouldThrowInvalidVerificationToken_whenTokenExpired() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            when(authService.verifyEmail(any(com.backend.dto.auth.VerifyEmailRequest.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            400, "INVALID_OR_EXPIRED_VERIFICATION_TOKEN",
                            "Invalid or expired verification token"));

            com.backend.dto.auth.VerifyEmailRequest verifyReq =
                    new com.backend.dto.auth.VerifyEmailRequest("expired-token");

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.verifyEmail(verifyReq)
            );
            assertEquals(400, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("POST /auth/resend-verification")
    class ResendVerification {

        @Test
        @DisplayName("should return 204 when email known")
        void shouldReturn204_whenEmailKnown() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            doNothing().when(authService).resendVerification(any(com.backend.dto.auth.ResendVerificationRequest.class));

            com.backend.dto.auth.ResendVerificationRequest req =
                    new com.backend.dto.auth.ResendVerificationRequest("host@test.com");

            Response response = resource.resendVerification(req);

            assertEquals(204, response.getStatus());
        }
    }

    @Nested
    @DisplayName("POST /auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("should return 204 when email known")
        void shouldReturn204_whenEmailKnown() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            doNothing().when(authService).forgotPassword(any(com.backend.dto.auth.ForgotPasswordRequest.class));

            com.backend.dto.auth.ForgotPasswordRequest req =
                    new com.backend.dto.auth.ForgotPasswordRequest("host@test.com");

            Response response = resource.forgotPassword(req);

            assertEquals(204, response.getStatus());
        }
    }

    @Nested
    @DisplayName("POST /auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("should return 204 when token valid")
        void shouldReturn204_whenTokenValid() {
            authService = Mockito.mock(AuthService.class);
            resource = new AuthResource(authService);

            doNothing().when(authService).resetPassword(any(com.backend.dto.auth.ResetPasswordRequest.class));

            com.backend.dto.auth.ResetPasswordRequest req =
                    new com.backend.dto.auth.ResetPasswordRequest("valid-token", "N3w!StrongPass");

            Response response = resource.resetPassword(req);

            assertEquals(204, response.getStatus());
        }
    }
}