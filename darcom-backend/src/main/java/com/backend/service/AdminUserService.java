package com.backend.service;

import com.backend.domain.User;
import com.backend.domain.enums.ListingStatus;
import com.backend.domain.enums.UserRole;
import com.backend.dto.common.PageResponse;
import com.backend.dto.user.BlockUserRequest;
import com.backend.dto.user.UserDetailDto;
import com.backend.dto.user.UserDto;
import com.backend.dto.user.UserSummaryDto;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.persistence.Tx;
import com.backend.repository.Pageable;
import com.backend.repository.RefreshTokenRepository;
import com.backend.repository.ReportRepository;
import com.backend.repository.UserRepository;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Admin user management. Blocking revokes sessions and hides the host's ACTIVE listings;
 * unblocking never restores listings.
 */
@Singleton
public class AdminUserService {

    private static final Logger LOG = Logger.getLogger(AdminUserService.class.getName());

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "email", "fullName", "role");
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public record Sort(String field, boolean asc) {
    }

    public PageResponse<UserSummaryDto> list(String role, Boolean verified, Boolean blocked, String search,
                                             int page, int size, Sort sort) {
        if (page < 0 || size < 1) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "page must be >= 0 and size must be >= 1");
        }
        int effectiveSize = Math.min(size, MAX_SIZE);
        UserRole roleFilter = parseRole(role);

        return Tx.inRead(em -> {
            UserRepository users = new UserRepository(em);
            List<User> found = users.search(roleFilter, verified, blocked, search,
                    new Pageable(page, effectiveSize), sort.field(), sort.asc());
            long total = users.countSearch(roleFilter, verified, blocked, search);
            List<UserSummaryDto> items = found.stream().map(UserSummaryDto::from).toList();
            return PageResponse.of(items, page, effectiveSize, total);
        });
    }

    public UserDetailDto getById(UUID id) {
        return Tx.inRead(em -> {
            User user = new UserRepository(em).findById(id).orElse(null);
            if (user == null) {
                throw new ApiException(404, ErrorCode.USER_NOT_FOUND, "User not found");
            }
            return UserDetailDto.from(user, listingCount(em, id),
                    new ReportRepository(em).countByReportedHost(user));
        });
    }

    public UserDto block(UUID adminId, UUID id, BlockUserRequest request) {
        return Tx.inTransaction(em -> {
            User user = new UserRepository(em).findById(id).orElse(null);
            if (user == null) {
                throw new ApiException(404, ErrorCode.USER_NOT_FOUND, "User not found");
            }
            if (user.getRole() == UserRole.ADMIN) {
                throw new ApiException(409, ErrorCode.CANNOT_BLOCK_ADMIN, "Admin accounts cannot be blocked");
            }
            if (user.isBlocked()) {
                throw new ApiException(409, ErrorCode.ALREADY_BLOCKED, "User is already blocked");
            }
            user.setBlocked(true);
            new RefreshTokenRepository(em).deleteAllByUserId(id);
            if (user.getRole() == UserRole.HOST) {
                em.createQuery("UPDATE Listing l SET l.status = :hidden WHERE l.host.id = :id AND l.status = :active")
                        .setParameter("hidden", ListingStatus.HIDDEN)
                        .setParameter("id", id)
                        .setParameter("active", ListingStatus.ACTIVE)
                        .executeUpdate();
            }
            LOG.info(() -> "admin=" + adminId + " blocked user=" + id + " reason=" + request.reason());
            return UserDto.from(user);
        });
    }

    public UserDto unblock(UUID id) {
        return Tx.inTransaction(em -> {
            User user = new UserRepository(em).findById(id).orElse(null);
            if (user == null) {
                throw new ApiException(404, ErrorCode.USER_NOT_FOUND, "User not found");
            }
            if (!user.isBlocked()) {
                throw new ApiException(409, ErrorCode.NOT_BLOCKED, "User is not blocked");
            }
            user.setBlocked(false);
            return UserDto.from(user);
        });
    }

    /** Parses {@code sort=field,dir}; default {@code createdAt,desc}; anything else → 400. */
    public static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new Sort("createdAt", false);
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR,
                    "Invalid sort. Allowed fields: createdAt, email, fullName, role with direction asc|desc");
        }
        boolean asc;
        if ("asc".equalsIgnoreCase(parts[1])) {
            asc = true;
        } else if ("desc".equalsIgnoreCase(parts[1])) {
            asc = false;
        } else {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR,
                    "Invalid sort. Allowed fields: createdAt, email, fullName, role with direction asc|desc");
        }
        return new Sort(parts[0], asc);
    }

    public static UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR,
                    "Invalid role. Allowed values: VISITOR, HOST, ADMIN");
        }
    }

    public static int parsePage(String page) {
        if (page == null || page.isBlank()) return 0;
        try {
            return Integer.parseInt(page.trim());
        } catch (NumberFormatException e) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Invalid page: must be an integer >= 0");
        }
    }

    public static int parseSize(String size) {
        if (size == null || size.isBlank()) return DEFAULT_SIZE;
        try {
            return Integer.parseInt(size.trim());
        } catch (NumberFormatException e) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Invalid size: must be an integer >= 1");
        }
    }

    public static Boolean parseFlag(String name, String value) {
        if (value == null || value.isBlank()) return null;
        if ("true".equalsIgnoreCase(value.trim())) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value.trim())) return Boolean.FALSE;
        throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Invalid " + name + ": must be true or false");
    }

    private static long listingCount(jakarta.persistence.EntityManager em, UUID hostId) {
        return em.createQuery("SELECT COUNT(l) FROM Listing l WHERE l.host.id = :id", Long.class)
                .setParameter("id", hostId)
                .getSingleResult();
    }
}
