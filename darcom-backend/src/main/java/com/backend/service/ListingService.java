package com.backend.service;

import com.backend.config.AppConfig;
import com.backend.domain.Listing;
import com.backend.domain.Photo;
import com.backend.domain.User;
import com.backend.dto.common.PageResponse;
import com.backend.dto.listing.CreateListingRequest;
import com.backend.dto.listing.ListingDetailDto;
import com.backend.dto.listing.ListingSummaryDto;
import com.backend.dto.listing.PhotoDto;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.domain.enums.ListingStatus;
import com.backend.persistence.Tx;
import com.backend.repository.ListingRepository;
import com.backend.repository.Pageable;
import com.backend.repository.UserRepository;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

@Singleton
public class ListingService {

    private final FileStorageService fileStorageService;
    private final AppConfig config;

    public ListingService(FileStorageService fileStorageService, AppConfig config) {
        this.fileStorageService = fileStorageService;
        this.config = config;
    }

    public ListingDetailDto create(UUID hostId, CreateListingRequest req) {
        return Tx.inTransaction(em -> {
            UserRepository userRepository = new UserRepository(em);
            User host = userRepository.findById(hostId).orElse(null);
            if (host == null) {
                throw new ApiException(404, ErrorCode.USER_NOT_FOUND, "Host not found");
            }
            if (!host.isVerified() && host.getRole() == com.backend.domain.enums.UserRole.HOST) {
                throw new ApiException(403, ErrorCode.EMAIL_NOT_VERIFIED, "Host email is not verified");
            }

            Listing listing = new Listing();
            listing.setHost(host);
            listing.setTitle(req.title());
            listing.setDescription(req.description());
            listing.setCity(req.city());
            listing.setAddress(req.address());
            listing.setPricePerNight(req.pricePerNight());
            listing.setCurrency(req.currency() != null ? req.currency() : "MAD");
            listing.setMealsIncluded(req.mealsIncluded());
            listing.setActivities(req.activities());
            listing.setStatus(ListingStatus.ACTIVE);

            ListingRepository repo = new ListingRepository(em);
            repo.create(listing);
            return ListingDetailDto.from(listing);
        });
    }

    public Response listPublic(String city, BigDecimal minPrice, BigDecimal maxPrice,
                                Boolean mealsIncluded, int page, int size, String sort) {
        Pageable pageable = new Pageable(page, size);
        String sortField = sort != null && !sort.isBlank() ? sort : "createdAt";

        return Tx.inRead(em -> {
            ListingRepository repo = new ListingRepository(em);
            List<Listing> results = repo.search(city, minPrice, maxPrice, mealsIncluded,
                    pageable, sortField, true);

            long total = repo.countSearch(city, minPrice, maxPrice, mealsIncluded);

            List<ListingSummaryDto> dtos = results.stream()
                    .map(ListingSummaryDto::from)
                    .toList();

            PageResponse<ListingSummaryDto> pageResponse = PageResponse.of(dtos, pageable.page(), pageable.size(), total);
            return Response.ok(pageResponse).build();
        });
    }

    public ListingDetailDto getById(UUID id) {
        return Tx.inRead(em -> {
            ListingRepository repo = new ListingRepository(em);
            Listing listing = repo.findById(id).orElse(null);
            if (listing == null || listing.getStatus() == ListingStatus.REMOVED) {
                throw new ApiException(404, ErrorCode.LISTING_NOT_FOUND, "Listing not found");
            }
            if (listing.getStatus() == ListingStatus.HIDDEN) {
                throw new ApiException(403, ErrorCode.LISTING_NOT_FOUND, "Listing not available");
            }

            return ListingDetailDto.from(listing);
        });
    }

    public ListingDetailDto update(UUID id, UUID hostId, CreateListingRequest req) {
        return Tx.inTransaction(em -> {
            ListingRepository repo = new ListingRepository(em);
            Listing listing = repo.findById(id).orElse(null);
            if (listing == null) {
                throw new ApiException(404, ErrorCode.LISTING_NOT_FOUND, "Listing not found");
            }
            if (!listing.getHost().getId().equals(hostId)) {
                throw new ApiException(403, ErrorCode.NOT_OWNER, "You don't own this listing");
            }

            listing.setTitle(req.title());
            listing.setDescription(req.description());
            listing.setCity(req.city());
            listing.setAddress(req.address());
            listing.setPricePerNight(req.pricePerNight());
            listing.setCurrency(req.currency() != null ? req.currency() : "MAD");
            listing.setMealsIncluded(req.mealsIncluded());
            listing.setActivities(req.activities());

            repo.update(listing);
            return ListingDetailDto.from(listing);
        });
    }

    public Response delete(UUID id, UUID hostId) {
        return Tx.inTransaction(em -> {
            ListingRepository repo = new ListingRepository(em);
            Listing listing = repo.findById(id).orElse(null);
            if (listing == null) {
                throw new ApiException(404, ErrorCode.LISTING_NOT_FOUND, "Listing not found");
            }
            if (!listing.getHost().getId().equals(hostId)) {
                throw new ApiException(403, ErrorCode.NOT_OWNER, "You don't own this listing");
            }
            if (repo.hasActiveBookings(id)) {
                throw new ApiException(409, ErrorCode.CANNOT_DELETE_WITH_ACTIVE_BOOKINGS, "Cannot delete listing with active bookings");
            }

            listing.setStatus(ListingStatus.REMOVED);
            repo.update(listing);
            return Response.noContent().build();
        });
    }

    public Response listMine(UUID hostId, int page, int size, String sort) {
        Pageable pageable = new Pageable(page, size);
        String sortField = sort != null && !sort.isBlank() ? sort : "createdAt";

        return Tx.inRead(em -> {
            ListingRepository repo = new ListingRepository(em);
            List<Listing> results = repo.findByHost(hostId, pageable, sortField, true);
            long total = repo.countByHost(hostId);

            List<ListingSummaryDto> dtos = results.stream()
                    .map(ListingSummaryDto::from)
                    .toList();

            PageResponse<ListingSummaryDto> pageResponse = PageResponse.of(dtos, pageable.page(), pageable.size(), total);
            return Response.ok(pageResponse).build();
        });
    }

    public PhotoDto addPhoto(UUID listingId, UUID hostId, InputStream stream, FormDataContentDisposition meta) {
        return Tx.inTransaction(em -> {
            ListingRepository repo = new ListingRepository(em);
            Listing listing = repo.findById(listingId).orElse(null);
            if (listing == null) {
                throw new ApiException(404, ErrorCode.LISTING_NOT_FOUND, "Listing not found");
            }
            if (!listing.getHost().getId().equals(hostId)) {
                throw new ApiException(403, ErrorCode.NOT_OWNER, "You don't own this listing");
            }

            String storedFilename = fileStorageService.storeImage(stream, meta.getFileName());

            Photo photo = new Photo();
            int sortOrder = listing.getPhotos().size();
            photo.setListing(listing);
            photo.setUrl(storedFilename);
            photo.setSortOrder(sortOrder);

            listing.getPhotos().add(photo);
            repo.update(listing);

            return PhotoDto.from(photo);
        });
    }

    public Response deletePhoto(UUID listingId, UUID photoId, UUID hostId) {
        return Tx.inTransaction(em -> {
            ListingRepository repo = new ListingRepository(em);
            Listing listing = repo.findById(listingId).orElse(null);
            if (listing == null) {
                throw new ApiException(404, ErrorCode.LISTING_NOT_FOUND, "Listing not found");
            }
            if (!listing.getHost().getId().equals(hostId)) {
                throw new ApiException(403, ErrorCode.NOT_OWNER, "You don't own this listing");
            }

            Photo photo = listing.getPhotos().stream()
                    .filter(p -> p.getId().equals(photoId))
                    .findFirst()
                    .orElse(null);
            if (photo == null) {
                throw new ApiException(404, ErrorCode.LISTING_NOT_FOUND, "Photo not found");
            }

            fileStorageService.deleteImage(photo.getUrl());
            listing.getPhotos().remove(photo);
            repo.update(listing);

            return Response.noContent().build();
        });
    }
}