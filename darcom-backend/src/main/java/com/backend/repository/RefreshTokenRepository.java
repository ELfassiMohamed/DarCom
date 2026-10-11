package com.backend.repository;

import com.backend.domain.RefreshToken;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class RefreshTokenRepository extends GenericRepository<RefreshToken> {

    public RefreshTokenRepository(EntityManager em) {
        super(em, RefreshToken.class);
    }

    /** Persists a new refresh-token row. */
    public RefreshToken save(RefreshToken token) {
        return create(token);
    }

    /**
     * POST /auth/refresh — looked up by the hash of the presented token, never the raw value.
     * The user is fetch-joined (the association is LAZY) so DTO mapping works after the EM closes.
     */
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return em.createQuery(
                "SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.tokenHash = :hash",
                        RefreshToken.class)
                .setParameter("hash", tokenHash)
                .getResultStream()
                .findFirst();
    }

    /** POST /auth/logout: revoke one token, scoped to its owner (idempotent — returns rows deleted). */
    public int deleteByTokenHashAndUserId(String tokenHash, UUID userId) {
        return em.createQuery(
                "DELETE FROM RefreshToken rt WHERE rt.tokenHash = :hash AND rt.user.id = :userId")
                .setParameter("hash", tokenHash)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    /** POST /auth/logout legacy single-arg form (unscoped). Prefer the two-arg form. */
    public void deleteByTokenHash(String tokenHash) {
        findByTokenHash(tokenHash).ifPresent(this::delete);
    }

    /** Blocking a user revokes every session: delete all of that user's refresh tokens. */
    public int deleteAllByUserId(UUID userId) {
        return em.createQuery("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    /** POST /auth/login cleanup: drop the user's expired tokens so they do not accumulate forever. */
    public int deleteExpiredByUserId(UUID userId) {
        return em.createQuery(
                "DELETE FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.expiresAt < :now")
                .setParameter("userId", userId)
                .setParameter("now", Instant.now())
                .executeUpdate();
    }

    /**
     * Optional cleanup — expired tokens otherwise accumulate forever. Not
     * called from anywhere yet; wiring it to a scheduled job is later work
     * if you want it. Bulk JPQL DELETE bypasses the persistence context, so
     * any RefreshToken already loaded in this EntityManager won't reflect
     * the deletion until it's reloaded.
     */
    public int deleteAllExpired() {
        return em.createQuery("DELETE FROM RefreshToken rt WHERE rt.expiresAt < CURRENT_TIMESTAMP")
                .executeUpdate();
    }
}
