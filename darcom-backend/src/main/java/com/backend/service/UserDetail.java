package com.backend.service;

import com.backend.domain.User;

/** Admin user detail: the user plus the two counts the spec's detail view requires. Service-owned carrier (Conversation precedent). */
public record UserDetail(User user, long listingCount, long reportCount) {
}
