package com.backend.service;

import com.backend.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public final class TransactionRunner {

    private TransactionRunner() {
    }

    @FunctionalInterface
    public interface Work<T> {
        T run(EntityManager em);
    }

    @FunctionalInterface
    public interface VoidWork {
        void run(EntityManager em);
    }

    public static <T> T call(Work<T> work) {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.run(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static void execute(VoidWork work) {
        call(em -> {
            work.run(em);
            return null;
        });
    }
}
