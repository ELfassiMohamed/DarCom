package com.backend.service;

import com.backend.domain.User;
import com.backend.exception.NotFoundException;
import com.backend.repository.UserRepository;

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
}
