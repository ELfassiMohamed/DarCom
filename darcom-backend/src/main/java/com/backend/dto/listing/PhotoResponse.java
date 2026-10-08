package com.backend.dto.listing;

import com.backend.domain.Photo;

import java.util.UUID;

/** Nested shape only — there are still no photo endpoints (§0). Order follows the entity's @OrderBy(sortOrder ASC); from() preserves list order. */
public record PhotoResponse(UUID id, String url, int sortOrder) {
    public static PhotoResponse from(Photo photo) {
        return new PhotoResponse(photo.getId(), photo.getUrl(), photo.getSortOrder());
    }
}
