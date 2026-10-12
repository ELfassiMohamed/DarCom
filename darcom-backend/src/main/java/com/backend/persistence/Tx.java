package com.backend.persistence;

import jakarta.persistence.EntityManager;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Short-lived {@link EntityManager} per operation: open → work → close in
 * {@code finally}. Never shared across requests. Resource-local transactions.
 */
public final class Tx {

    private Tx() {
    }

    public static <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager em = JpaUtil.createEntityManager();
        try {
            em.getTransaction().begin();
            try {
                T result = work.apply(em);
                em.getTransaction().commit();
                return result;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        } finally {
            em.close();
        }
    }

    public static void inTransactionVoid(Consumer<EntityManager> work) {
        inTransaction(em -> {
            work.accept(em);
            return null;
        });
    }

    /** Read-only operation with its own short-lived {@link EntityManager}. */
    public static <T> T inRead(Function<EntityManager, T> work) {
        EntityManager em = JpaUtil.createEntityManager();
        try {
            return work.apply(em);
        } finally {
            em.close();
        }
    }
}
