package com.backend.repository;

import com.backend.domain.AuthToken;
import com.backend.domain.enums.TokenPurpose;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class AuthTokenRepository extends GenericRepository<AuthToken> {

    public AuthTokenRepository(EntityManager em) {
        super(em, AuthToken.class);
    }

    public AuthToken save(AuthToken token) {
        return create(token);
    }

    /** Looked up by hash of the presented token; user fetch-joined (LAZY) for post-EM use. */
    public Optional<AuthToken> findByTokenHash(String tokenHash) {
        return em.createQuery(
                "SELECT t FROM AuthToken t JOIN FETCH t.user WHERE t.tokenHash = :hash", AuthToken.class)
                .setParameter("hash", tokenHash)
                .getResultStream()
                .findFirst();
    }

    /** How many tokens of this purpose were issued to the user since {@code since} (rate limiting). */
    public long countRecentByUserAndPurpose(UUID userId, TokenPurpose purpose, Instant since) {
        return em.createQuery(
                "SELECT COUNT(t) FROM AuthToken t WHERE t.user.id = :userId AND t.purpose = :purpose AND t.createdAt >= :since",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("purpose", purpose)
                .setParameter("since", since)
                .getSingleResult();
    }

    /** Issuing a new token invalidates previous ones of the same purpose. */
    public int deleteAllByUserAndPurpose(UUID userId, TokenPurpose purpose) {
        return em.createQuery("DELETE FROM AuthToken t WHERE t.user.id = :userId AND t.purpose = :purpose")
                .setParameter("userId", userId)
                .setParameter("purpose", purpose)
                .executeUpdate();
    }

    public int deleteExpiredByUserAndPurpose(UUID userId, TokenPurpose purpose) {
        return em.createQuery(
                "DELETE FROM AuthToken t WHERE t.user.id = :userId AND t.purpose = :purpose AND t.expiresAt < :now")
                .setParameter("userId", userId)
                .setParameter("purpose", purpose)
                .setParameter("now", Instant.now())
                .executeUpdate();
    }
}
