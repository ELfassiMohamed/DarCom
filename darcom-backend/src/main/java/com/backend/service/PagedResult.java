package com.backend.service;

import java.util.List;

public record PagedResult<T>(List<T> items, long totalItems) {
}
