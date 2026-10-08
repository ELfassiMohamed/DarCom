package com.backend.dto;

import java.util.List;

/**
 * The §1 pagination envelope, shared by every collection endpoint from here on.
 * page/size are echoed back as applied (see ListingResource §3.5); totalPages is arithmetic here
 * so no resource can compute it inconsistently.
 */
public record PagedResponse<T>(List<T> items, int page, int size, long totalItems, long totalPages) {
    public PagedResponse {
        totalPages = (size <= 0 || totalItems <= 0) ? 0 : (totalItems + size - 1) / size;
    }

    /** Normal construction path: totalPages is always derived, never supplied. */
    public PagedResponse(List<T> items, int page, int size, long totalItems) {
        this(items, page, size, totalItems, 0L);
    }
}
