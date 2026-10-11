package com.backend.security;

import com.backend.domain.User;
import com.backend.domain.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-must-be-at-least-32-bytes-long!";
    private static final String OTHER_SECRET = "other-secret-must-be-at-least-32-bytes-lo!";

    private static JwtService service() {
        return new JwtService(SECRET, "darcom", 900, 604800);
    }

    private static User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("jwt@example.com");
        user.setRole(UserRole.HOST);
        return user;
    }

    @Test
    void accessTokenRoundTrip() {
        JwtService jwt = service();
        User user = user();
        String token = jwt.createAccessToken(user);
        JwtService.AccessClaims claims = jwt.parseAccess(token);
        assertEquals(user.getId(), claims.userId());
        assertEquals("HOST", claims.role());
    }

    @Test
    void refreshTokenRoundTrip() {
        JwtService jwt = service();
        User user = user();
        String token = jwt.createRefreshToken(user);
        assertEquals(user.getId(), jwt.parseRefresh(token));
    }

    @Test
    void twoRefreshTokensNeverCollide() {
        JwtService jwt = service();
        User user = user();
        assertNotEquals(jwt.createRefreshToken(user), jwt.createRefreshToken(user));
    }

    @Test
    void expiredTokenRejected() {
        JwtService jwt = new JwtService(SECRET, "darcom", -10, -10);
        User user = user();
        assertThrows(InvalidTokenException.class, () -> jwt.parseAccess(jwt.createAccessToken(user)));
        assertThrows(InvalidTokenException.class, () -> jwt.parseRefresh(jwt.createRefreshToken(user)));
    }

    @Test
    void wrongSignatureRejected() {
        JwtService jwt = service();
        String token = jwt.createAccessToken(user());
        JwtService other = new JwtService(OTHER_SECRET, "darcom", 900, 604800);
        assertThrows(InvalidTokenException.class, () -> other.parseAccess(token));
    }

    @Test
    void wrongTypRejected() {
        JwtService jwt = service();
        User user = user();
        String refresh = jwt.createRefreshToken(user);
        String access = jwt.createAccessToken(user);
        assertThrows(InvalidTokenException.class, () -> jwt.parseAccess(refresh));
        assertThrows(InvalidTokenException.class, () -> jwt.parseRefresh(access));
    }

    @Test
    void wrongIssuerRejected() {
        JwtService jwt = service();
        String token = jwt.createAccessToken(user());
        JwtService otherIssuer = new JwtService(SECRET, "someone-else", 900, 604800);
        assertThrows(InvalidTokenException.class, () -> otherIssuer.parseAccess(token));
    }

    @Test
    void garbageRejected() {
        JwtService jwt = service();
        assertThrows(InvalidTokenException.class, () -> jwt.parseAccess("not-a-token"));
        assertThrows(InvalidTokenException.class, () -> jwt.parseRefresh(""));
    }
}
