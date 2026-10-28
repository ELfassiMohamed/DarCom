package com.backend.resource;

import com.backend.dto.listing.CreateListingRequest;
import com.backend.dto.listing.ListingDetailDto;
import com.backend.dto.listing.ListingSummaryDto;
import com.backend.dto.listing.PhotoDto;
import com.backend.dto.common.PageResponse;
import com.backend.domain.enums.ListingStatus;
import com.backend.security.AuthenticatedUser;
import com.backend.domain.enums.UserRole;
import com.backend.service.ListingService;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ListingResource")
class ListingResourceTest {

    private ListingService listingService;
    private ListingResource resource;

    private AuthenticatedUser hostPrincipal() {
        return new AuthenticatedUser(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                "host@test.com",
                UserRole.HOST
        );
    }

    private AuthenticatedUser visitorPrincipal() {
        return new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "visitor@test.com",
                UserRole.VISITOR
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

    private CreateListingRequest validRequest() {
        Set<String> activities = new LinkedHashSet<>();
        activities.add("Airport pickup");
        activities.add("Guided medina tour");

        return new CreateListingRequest(
                "Cozy riad near the medina",
                "Two-bedroom riad, 10 min walk to the stadium.",
                "Tetouan",
                "12 Rue de la Kasbah",
                new BigDecimal("450.00"),
                "MAD",
                true,
                activities
        );
    }

    private ListingDetailDto validListingDto() {
        Set<String> activities = new LinkedHashSet<>();
        activities.add("Airport pickup");
        activities.add("Guided medina tour");

        return new ListingDetailDto(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                new com.backend.dto.listing.HostSummaryDto(
                        UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                        "Youssef El Amrani"
                ),
                "Cozy riad near the medina",
                "Two-bedroom riad, 10 min walk to the stadium.",
                "Tetouan",
                "12 Rue de la Kasbah",
                new BigDecimal("450.00"),
                "MAD",
                true,
                activities,
                ListingStatus.ACTIVE,
                List.of(),
                java.time.Instant.parse("2026-10-12T13:00:00Z"),
                null
        );
    }

    private ListingSummaryDto validSummaryDto() {
        return new ListingSummaryDto(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                "Cozy riad near the medina",
                "Tetouan",
                new BigDecimal("450.00"),
                "MAD",
                "photo123.jpg",
                "Youssef El Amrani"
        );
    }

    @Nested
    @DisplayName("POST /listings")
    class CreateListing {

        @Test
        @DisplayName("should return 201 when host is verified and request is valid")
        void shouldReturn201_whenHostIsVerifiedAndRequestValid() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            when(listingService.create(any(UUID.class), any(CreateListingRequest.class)))
                    .thenReturn(validListingDto());

            Response response = resource.create(validRequest(), securityContext(hostPrincipal()));

            assertEquals(201, response.getStatus());
            verify(listingService).create(
                    eq(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")),
                    any(CreateListingRequest.class)
            );
        }

        @Test
        @DisplayName("should throw EMAIL_NOT_VERIFIED when host email is not verified")
        void shouldThrowEmailNotVerified_whenHostIsNotVerified() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            when(listingService.create(any(UUID.class), any(CreateListingRequest.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            403, "EMAIL_NOT_VERIFIED", "Host email is not verified"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.create(validRequest(),
                            securityContext(new AuthenticatedUser(UUID.randomUUID(), "unverified@test.com", UserRole.HOST)))
            );
            assertEquals(403, ex.getStatus());
            assertEquals("EMAIL_NOT_VERIFIED", ex.getCode());
        }

        @Test
        @DisplayName("should throw NPE when no authentication")
        void shouldThrowNpe_whenNoAuthentication() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            assertThrows(NullPointerException.class,
                    () -> resource.create(validRequest(), securityContext(null)));
        }
    }

    @Nested
    @DisplayName("GET /listings")
    class ListPublic {
        // Public list tests require full Jersey Test Framework with HK2 DI
        // to properly test query parameter parsing. Direct invocation
        // bypasses the full JAX-RS request pipeline.
    }

    @Nested
    @DisplayName("GET /listings/mine")
    class ListMine {
        // List/mine tests require full Jersey Test Framework with HK2 DI
        // to properly test role-based access control. Direct invocation
        // bypasses @RolesAllowed annotation processing.
    }

    @Nested
    @DisplayName("GET /listings/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 with full listing details")
        void shouldReturn200_whenListingExists() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            when(listingService.getById(listingId)).thenReturn(validListingDto());

            Response response = resource.getById(listingId);

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw LISTING_NOT_FOUND when listing not found")
        void shouldThrowListingNotFound_whenListingNotFound() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.randomUUID();
            when(listingService.getById(listingId))
                    .thenThrow(new com.backend.exception.ApiException(
                            404, "LISTING_NOT_FOUND", "Listing not found"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.getById(listingId)
            );
            assertEquals(404, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("PUT /listings/{id}")
    class UpdateListing {

        @Test
        @DisplayName("should return 200 when owner updates listing")
        void shouldReturn200_whenOwnerUpdatesListing() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            when(listingService.update(
                    eq(listingId), eq(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")),
                    any(CreateListingRequest.class)
            )).thenReturn(validListingDto());

            Response response = resource.update(listingId, validRequest(), securityContext(hostPrincipal()));

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should throw NOT_OWNER when not owner")
        void shouldThrowNotOwner_whenNotOwner() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.randomUUID();
            when(listingService.update(
                    eq(listingId), any(UUID.class), any(CreateListingRequest.class)
            )).thenThrow(new com.backend.exception.ApiException(
                    403, "NOT_OWNER", "You don't own this listing"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.update(listingId, validRequest(), securityContext(visitorPrincipal()))
            );
            assertEquals(403, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("DELETE /listings/{id}")
    class DeleteListing {

        @Test
        @DisplayName("should return 204 when owner deletes listing")
        void shouldReturn204_whenOwnerDeletesListing() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            when(listingService.delete(listingId, UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")))
                    .thenReturn(Response.noContent().build());

            Response response = resource.delete(listingId, securityContext(hostPrincipal()));

            assertEquals(204, response.getStatus());
        }

        @Test
        @DisplayName("should throw NOT_OWNER when not owner")
        void shouldThrowNotOwner_whenNotOwner() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.randomUUID();
            when(listingService.delete(eq(listingId), any(UUID.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            403, "NOT_OWNER", "You don't own this listing"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.delete(listingId, securityContext(visitorPrincipal()))
            );
            assertEquals(403, ex.getStatus());
        }

        @Test
        @DisplayName("should throw CANNOT_DELETE_WITH_ACTIVE_BOOKINGS when has active bookings")
        void shouldThrowCannotDelete_whenHasActiveBookings() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.randomUUID();
            when(listingService.delete(eq(listingId), any(UUID.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            409, "CANNOT_DELETE_WITH_ACTIVE_BOOKINGS",
                            "Cannot delete listing with active bookings"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.delete(listingId, securityContext(hostPrincipal()))
            );
            assertEquals(409, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("POST /listings/{id}/photos")
    class UploadPhoto {

        @Test
        @DisplayName("should return 201 when valid photo uploaded")
        void shouldReturn201_whenValidPhotoUploaded() throws Exception {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            PhotoDto photoDto = new PhotoDto(UUID.randomUUID(), "photo-uuid-123.jpg", 0);

            when(listingService.addPhoto(
                    eq(listingId), any(UUID.class), any(InputStream.class), any()
            )).thenReturn(photoDto);

            FormDataContentDisposition disposition = FormDataContentDisposition.name("file")
                    .fileName("test.jpg")
                    .build();

            Response response = resource.uploadPhoto(
                    listingId,
                    securityContext(hostPrincipal()),
                    new ByteArrayInputStream(new byte[]{1, 2, 3}),
                    disposition
            );

            assertEquals(201, response.getStatus());
        }

        @Test
        @DisplayName("should throw INVALID_FILE_TYPE when invalid file type")
        void shouldThrowInvalidFileType_whenInvalidFileType() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.randomUUID();
            when(listingService.addPhoto(
                    eq(listingId), any(UUID.class), any(InputStream.class), any()
            )).thenThrow(new com.backend.exception.ApiException(
                    400, "INVALID_FILE_TYPE", "Invalid file type"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.uploadPhoto(
                            listingId,
                            securityContext(hostPrincipal()),
                            new ByteArrayInputStream(new byte[]{1, 2, 3}),
                            FormDataContentDisposition.name("file").fileName("test.exe").build()
                    )
            );
            assertEquals(400, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("DELETE /listings/{id}/photos/{photoId}")
    class DeletePhoto {

        @Test
        @DisplayName("should return 204 when owner deletes photo")
        void shouldReturn204_whenOwnerDeletesPhoto() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            UUID photoId = UUID.randomUUID();
            when(listingService.deletePhoto(listingId, photoId, UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")))
                    .thenReturn(Response.noContent().build());

            Response response = resource.deletePhoto(listingId, photoId, securityContext(hostPrincipal()));

            assertEquals(204, response.getStatus());
        }

        @Test
        @DisplayName("should throw NOT_OWNER when not owner")
        void shouldThrowNotOwner_whenNotOwner() {
            listingService = Mockito.mock(ListingService.class);
            resource = new ListingResource(listingService);

            UUID listingId = UUID.randomUUID();
            UUID photoId = UUID.randomUUID();
            when(listingService.deletePhoto(eq(listingId), eq(photoId), any(UUID.class)))
                    .thenThrow(new com.backend.exception.ApiException(
                            403, "NOT_OWNER", "You don't own this listing"));

            com.backend.exception.ApiException ex = assertThrows(
                    com.backend.exception.ApiException.class,
                    () -> resource.deletePhoto(listingId, photoId, securityContext(visitorPrincipal()))
            );
            assertEquals(403, ex.getStatus());
        }
    }
}