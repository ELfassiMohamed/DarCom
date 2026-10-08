package com.backend.security;

import com.backend.domain.User;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

public final class JwtService {

    /** Exposed so responses can state the TTL (spec `expiresIn`) without a second literal. */
    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
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

    /**
     * Parses and validates an access token, returning the user id it was issued
     * for. Throws a JwtException subtype (ExpiredJwtException, SignatureException,
     * MalformedJwtException, ...) for anything wrong with it — deciding what that
     * means for the response is the caller's job (AuthenticationFilter, §5.1),
     * not this method's.
     */
    public static UUID verify(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token);
        return UUID.fromString(jws.getPayload().getSubject());
    }
}
