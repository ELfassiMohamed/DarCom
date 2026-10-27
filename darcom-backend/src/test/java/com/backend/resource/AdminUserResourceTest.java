package com.backend.resource;

import com.backend.dto.common.PageResponse;
import com.backend.dto.user.BlockUserRequest;
import com.backend.dto.user.UserDetailDto;
import com.backend.dto.user.UserDto;
import com.backend.dto.user.UserSummaryDto;
import com.backend.security.AuthenticatedUser;
import com.backend.domain.enums.UserRole;
import com.backend.service.AdminUserService;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AdminUserResource")
class AdminUserResourceTest {

    private AdminUserService adminUserService;
    private AdminUserResource resource;

    private AuthenticatedUser adminPrincipal() {
        return new AuthenticatedUser(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "admin@test.com",
                UserRole.ADMIN
        );
    }

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

    private UserSummaryDto validSummaryDto() {
        return new UserSummaryDto(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                "host@test.com", "Test Host", "HOST", true, false,
                Instant.parse("2026-10-12T13:00:00Z")
        );
    }

    private UserDto validUserDto() {
        return new UserDto(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                "host@test.com", "Test Host", "+212600000000",
                "HOST", true, false,
                Instant.parse("2026-10-12T13:00:00Z")
        );
    }

    private UserDetailDto validDetailDto() {
        return new UserDetailDto(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                "host@test.com", "Test Host", "+212600000000",
                "HOST", true, false,
                Instant.parse("2026-10-12T13:00:00Z"), 3L, 1L
        );
    }

    @Nested
    @DisplayName("GET /admin/users")
    class ListUsers {

        @Test
        @DisplayName("should return 200 when ADMIN role")
        void shouldReturn200_whenAdminRole() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            when(adminUserService.list(
                    anyString(), any(Boolean.class), any(Boolean.class), anyString(),
                    anyInt(), anyInt(), any(AdminUserService.Sort.class)
            )).thenReturn(new PageResponse<>(List.of(validSummaryDto()), 0, 20, 1, 1));

            Response response = resource.list(
                    null, null, null, null, null, null, null
            );

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should return 200 when HOST role (no @RolesAllowed enforcement in direct call)")
        void shouldReturn200_whenHostRole() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            when(adminUserService.list(
                    anyString(), any(Boolean.class), any(Boolean.class), anyString(),
                    anyInt(), anyInt(), any(AdminUserService.Sort.class)
            )).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

            Response response = resource.list(
                    null, null, null, null, null, null, null
            );

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should return 200 with role filter")
        void shouldReturn200_withRoleFilter() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            when(adminUserService.list(
                    eq("HOST"), isNull(), isNull(), isNull(), anyInt(), anyInt(), any()
            )).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

            Response response = resource.list(
                    "HOST", null, null, null, null, null, null
            );

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw VALIDATION_ERROR when invalid sort")
        void shouldThrowValidationError_whenInvalidSort() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.list(
                            null, null, null, null, null, null, "invalid,dir"
                    )
            );
            assertEquals(400, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("GET /admin/users/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 when user exists")
        void shouldReturn200_whenUserExists() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            UUID userId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            when(adminUserService.getById(userId)).thenReturn(validDetailDto());

            Response response = resource.getById(userId);

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when user not found")
        void shouldThrowUserNotFound_whenUserNotFound() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            UUID userId = UUID.randomUUID();
            when(adminUserService.getById(userId))
                    .thenThrow(new com.backend.exception.ApiException(
                            404, "USER_NOT_FOUND", "User not found"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.getById(userId)
            );
            assertEquals(404, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("PATCH /admin/users/{id}/block")
    class BlockUser {

        @Test
        @DisplayName("should return 200 when blocking valid user")
        void shouldReturn200_whenBlockingValidUser() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            UUID userId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            BlockUserRequest request = new BlockUserRequest("Repeated fake listings");
            when(adminUserService.block(
                    any(UUID.class), eq(userId), any(BlockUserRequest.class)
            )).thenReturn(validUserDto());

            Response response = resource.block(userId, request, securityContext(adminPrincipal()));

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw ALREADY_BLOCKED when already blocked")
        void shouldThrowAlreadyBlocked_whenAlreadyBlocked() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            UUID userId = UUID.randomUUID();
            BlockUserRequest request = new BlockUserRequest("reason");
            when(adminUserService.block(
                    any(UUID.class), eq(userId), any(BlockUserRequest.class)
            )).thenThrow(new com.backend.exception.ApiException(
                    409, "ALREADY_BLOCKED", "User is already blocked"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.block(userId, request, securityContext(adminPrincipal()))
            );
            assertEquals(409, ex.getStatus());
        }

        @Test
        @DisplayName("should throw CANNOT_BLOCK_ADMIN when blocking ADMIN")
        void shouldThrowCannotBlockAdmin_whenBlockingAdmin() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            UUID adminId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            BlockUserRequest request = new BlockUserRequest("reason");
            when(adminUserService.block(
                    any(UUID.class), eq(adminId), any(BlockUserRequest.class)
            )).thenThrow(new com.backend.exception.ApiException(
                    409, "CANNOT_BLOCK_ADMIN", "Admin accounts cannot be blocked"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.block(adminId, request, securityContext(adminPrincipal()))
            );
            assertEquals(409, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("PATCH /admin/users/{id}/unblock")
    class UnblockUser {

        @Test
        @DisplayName("should return 200 when unblocking valid user")
        void shouldReturn200_whenUnblockingValidUser() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            UUID userId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            when(adminUserService.unblock(userId)).thenReturn(validUserDto());

            Response response = resource.unblock(userId);

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw NOT_BLOCKED when user not blocked")
        void shouldThrowNotBlocked_whenUserNotBlocked() {
            adminUserService = Mockito.mock(AdminUserService.class);
            resource = new AdminUserResource(adminUserService);

            UUID userId = UUID.randomUUID();
            when(adminUserService.unblock(userId))
                    .thenThrow(new com.backend.exception.ApiException(
                            409, "NOT_BLOCKED", "User is not blocked"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.unblock(userId)
            );
            assertEquals(409, ex.getStatus());
        }
    }
}