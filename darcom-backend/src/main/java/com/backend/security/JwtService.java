package com.backend.security;

import com.backend.domain.User;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

public final class JwtService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final SecretKey KEY = loadKey();

    private JwtService() {
    }

    private static SecretKey loadKey() {
        String secret = Dotenv.load().get("JWT_SECRET");
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /** Signed access token: sub = user id, a "role" claim, 15-minute expiry. */
    public static String issueAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_TTL.toMillis());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(KEY)
                .compact();
    }
}
