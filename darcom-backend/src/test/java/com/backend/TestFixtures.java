package com.backend;

import com.backend.domain.Listing;
import com.backend.domain.User;
import com.backend.domain.enums.ListingStatus;
import com.backend.domain.enums.UserRole;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TestFixtures {

    private TestFixtures() {}

    public static User host() {
        return user("host@test.com", "Test Host", UserRole.HOST, true, false);
    }

    public static User visitor() {
        return user("visitor@test.com", "Test Visitor", UserRole.VISITOR, true, false);
    }

    public static User admin() {
        return user("admin@test.com", "Test Admin", UserRole.ADMIN, true, false);
    }

    public static User unverifiedHost() {
        return user("unverified@test.com", "Unverified Host", UserRole.HOST, false, false);
    }

    public static User blockedHost() {
        return user("blocked@test.com", "Blocked Host", UserRole.HOST, true, true);
    }

    public static User user(String email, String fullName, UserRole role,
                            boolean verified, boolean blocked) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$hashed");
        u.setFullName(fullName);
        u.setRole(role);
        u.setVerified(verified);
        u.setBlocked(blocked);
        return u;
    }

    public static Listing activeListing(User host) {
        Listing l = new Listing();
        l.setHost(host);
        l.setTitle("Cozy riad near the medina");
        l.setDescription("Two-bedroom riad, 10 min walk to the stadium.");
        l.setCity("Tetouan");
        l.setAddress("12 Rue de la Kasbah");
        l.setPricePerNight(new BigDecimal("450.00"));
        l.setCurrency("MAD");
        l.setMealsIncluded(true);
        l.setActivities(new LinkedHashSet<>(List.of("Airport pickup", "Guided medina tour")));
        l.setStatus(ListingStatus.ACTIVE);
        return l;
    }
}