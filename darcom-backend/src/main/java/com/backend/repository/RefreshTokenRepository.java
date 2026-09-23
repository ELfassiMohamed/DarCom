package com.backend.repository;

import com.backend.domain.RefreshToken;
import jakarta.persistence.EntityManager;

import java.util.Optional;

public class RefreshTokenRepository extends GenericRepository<RefreshToken> {

    public RefreshTokenRepository(EntityManager em) {
        super(em, RefreshToken.class);
    }

    /** POST /auth/refresh — looked up by the hash of the presented token, never the raw value. */
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return em.createQuery(
                "SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :hash", RefreshToken.class)
                .setParameter("hash", tokenHash)
                .getResultStream()
                .findFirst();
    }

    /** POST /auth/logout: revoke one token. */
    public void deleteByTokenHash(String tokenHash) {
        findByTokenHash(tokenHash).ifPresent(this::delete);
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
