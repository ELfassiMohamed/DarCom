package com.backend.service;

import com.backend.domain.User;
import com.backend.dto.user.UpdateProfileRequest;
import com.backend.dto.user.UserDto;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.persistence.Tx;
import com.backend.repository.UserRepository;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class UserService {

    public UserDto getMe(UUID userId) {
        return Tx.inRead(em -> {
            User user = new UserRepository(em).findById(userId).orElse(null);
            if (user == null) {
                throw new ApiException(404, ErrorCode.USER_NOT_FOUND, "User not found");
            }
            return UserDto.from(user);
        });
    }

    /** PUT replaces the profile: an omitted/null phone clears it. */
    public UserDto updateMe(UUID userId, UpdateProfileRequest request) {
        return Tx.inTransaction(em -> {
            UserRepository users = new UserRepository(em);
            User user = users.findById(userId).orElse(null);
            if (user == null) {
                throw new ApiException(404, ErrorCode.USER_NOT_FOUND, "User not found");
            }
            user.setFullName(request.fullName());
            user.setPhone(request.phone());
            return UserDto.from(user);
        });
    }
}
