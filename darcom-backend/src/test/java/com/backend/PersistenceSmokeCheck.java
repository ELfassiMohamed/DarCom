package com.backend;

import com.backend.domain.User;
import com.backend.domain.enums.UserRole;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manual, throwaway check that entities actually round-trip through Postgres.
 * Not a JUnit test (there's no test framework in pom.xml yet) — just run main()
 * from your IDE. Delete this once you've confirmed persistence works.
 */
public class PersistenceSmokeCheck {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Adjust these three keys to whatever names your .env file actually uses.
        // If you already have a class that builds the EntityManagerFactory with
        // your real connection properties, use that instead of this block.
        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", dotenv.get("DB_URL"));
        props.put("jakarta.persistence.jdbc.user", dotenv.get("DB_USER"));
        props.put("jakarta.persistence.jdbc.password", dotenv.get("DB_PASSWORD"));
        props.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("invest-pu", props);
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            User user = new User();
            user.setEmail("smoke-check@example.com");
            user.setPasswordHash("not-a-real-hash");
            user.setFullName("Smoke Check");
            user.setRole(UserRole.VISITOR);

            em.persist(user);
            tx.commit();

            UUID savedId = user.getId();
            System.out.println("Persisted -> " + savedId);

            // Fresh EntityManager, so this read has to hit the DB rather than
            // just returning the object still sitting in the first one's cache.
            em.close();
            em = emf.createEntityManager();
            User reloaded = em.find(User.class, savedId);
            System.out.println("Reloaded from DB -> " + reloaded);

        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
            emf.close();
        }
    }
}