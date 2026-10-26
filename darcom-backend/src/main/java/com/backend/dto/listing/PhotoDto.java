package com.backend.dto.listing;

import java.util.UUID;

public record PhotoDto(

    UUID id,
    String url,
    int sortOrder
) {
    public static PhotoDto from(com.backend.domain.Photo photo) {
        return new PhotoDto(photo.getId(), photo.getUrl(), photo.getSortOrder());
    }
}