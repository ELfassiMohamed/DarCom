package com.backend.resource;

import com.backend.dto.user.UserDto;
import com.backend.dto.user.UpdateProfileRequest;
import com.backend.security.AuthenticatedUser;
import com.backend.domain.enums.UserRole;
import com.backend.service.UserService;
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

@DisplayName("UserResource")
class UserResourceTest {

    private UserService userService;
    private UserResource resource;

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

    private UserDto validUserDto() {
        return new UserDto(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                "host@test.com", "Test Host", "+212600000000",
                "HOST", true, false,
                Instant.parse("2026-10-12T13:00:00Z")
        );
    }

    @Nested
    @DisplayName("GET /users/me")
    class GetMe {

        @Test
        @DisplayName("should return 200 when authenticated")
        void shouldReturn200_whenAuthenticated() {
            userService = Mockito.mock(UserService.class);
            resource = new UserResource(userService);

            when(userService.getMe(
                    eq(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            )).thenReturn(validUserDto());

            Response response = resource.me(securityContext(hostPrincipal()));

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw NPE when not authenticated")
        void shouldThrowNpe_whenNotAuthenticated() {
            userService = Mockito.mock(UserService.class);
            resource = new UserResource(userService);

            assertThrows(NullPointerException.class,
                    () -> resource.me(securityContext(null)));
        }
    }

    @Nested
    @DisplayName("PUT /users/me")
    class UpdateMe {

        @Test
        @DisplayName("should return 200 when update valid")
        void shouldReturn200_whenUpdateValid() {
            userService = Mockito.mock(UserService.class);
            resource = new UserResource(userService);

            UpdateProfileRequest request = new UpdateProfileRequest("New Name", "+212611223344");
            when(userService.updateMe(
                    eq(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")),
                    any(UpdateProfileRequest.class)
            )).thenReturn(validUserDto());

            Response response = resource.updateMe(request, securityContext(hostPrincipal()));

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should return 200 when clearing phone (null)")
        void shouldReturn200_whenClearingPhone() {
            userService = Mockito.mock(UserService.class);
            resource = new UserResource(userService);

            UpdateProfileRequest request = new UpdateProfileRequest("New Name", null);
            when(userService.updateMe(
                    eq(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")),
                    any(UpdateProfileRequest.class)
            )).thenReturn(validUserDto());

            Response response = resource.updateMe(request, securityContext(hostPrincipal()));

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw NPE when not authenticated")
        void shouldThrowNpe_whenNotAuthenticated() {
            userService = Mockito.mock(UserService.class);
            resource = new UserResource(userService);

            UpdateProfileRequest request = new UpdateProfileRequest("New Name", null);
            assertThrows(NullPointerException.class,
                    () -> resource.updateMe(request, securityContext(null)));
        }
    }
}