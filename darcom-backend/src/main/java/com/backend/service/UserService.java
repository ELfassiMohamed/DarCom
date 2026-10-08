package com.backend.service;

import com.backend.domain.Listing;
import com.backend.domain.User;
import com.backend.domain.enums.ListingStatus;
import com.backend.domain.enums.UserRole;
import com.backend.exception.NotFoundException;
import com.backend.repository.ListingRepository;
import com.backend.repository.Pageable;
import com.backend.repository.ReportRepository;
import com.backend.repository.UserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Profile self-service. Deliberately tiny: only fullName + phone may change
 * here. Email/password are identity fields with their own flows (spec §5) —
 * this endpoint must never become a silent identity-hijack vector.
 */
public class UserService {

    /** PUT /users/me — updates fullName + phone only, returns the merged user. */
    public User updateProfile(User currentUser, String fullName, String phone) {
        return TransactionRunner.call(em -> {
            UserRepository repo = new UserRepository(em);
            User user = repo.findById(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Authenticated user not found"));
            user.setFullName(fullName);
            user.setPhone(phone);
            return repo.update(user);
        });
    }

    /** GET /admin/users — all four filters optional. */
    public PagedResult<User> getAll(UserRole role, Boolean verified, Boolean blocked,
                                    String searchText, Pageable pageable) {
        return TransactionRunner.call(em -> {
            UserRepository repo = new UserRepository(em);
            List<User> items = repo.search(role, verified, blocked, searchText, pageable);
            long totalItems = repo.countSearch(role, verified, blocked, searchText);
            return new PagedResult<>(items, totalItems);
        });
    }

    /** GET /admin/users/{id} — user plus listing/report counts. */
    public UserDetail getDetail(UUID userId) {
        return TransactionRunner.call(em -> {
            User user = new UserRepository(em).findById(userId)
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
            long listingCount = new ListingRepository(em).countByHost(user);
            long reportCount = new ReportRepository(em).countByReportedHost(user);
            return new UserDetail(user, listingCount, reportCount);
        });
    }

    /**
     * PATCH /admin/users/{id}/block — blocks and hides every ACTIVE listing in
     * the same tx (spec §10 side effect: a blocked host with live listings is
     * an inconsistent state). The request's reason is accepted for spec-shape
     * compatibility but not persisted — no reader exists for it anywhere in the
     * spec, and write-only columns are speculation, not audit.
     */
    public User block(UUID userId) {
        return TransactionRunner.call(em -> {
            UserRepository repo = new UserRepository(em);
            User user = repo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
            user.setBlocked(true);
            if (user.getRole() == UserRole.HOST) {
                for (Listing listing : new ListingRepository(em).findByHostAndStatus(user, ListingStatus.ACTIVE)) {
                    listing.setStatus(ListingStatus.HIDDEN);
                }
            }
            return repo.update(user);
        });
    }

    /**
     * PATCH /admin/users/{id}/unblock — clears blocked only. Hidden listings
     * are NOT restored (spec explicit): re-activation is a deliberate separate
     * admin action, exact behavior the spec mandates.
     */
    public User unblock(UUID userId) {
        return TransactionRunner.call(em -> {
            UserRepository repo = new UserRepository(em);
            User user = repo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
            user.setBlocked(false);
            return repo.update(user);
        });
    }
}
