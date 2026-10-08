package com.backend.persistence;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class JpaUtil {

    private static final String PERSISTENCE_UNIT_NAME = "invest-pu";
    private static final EntityManagerFactory EMF = buildEntityManagerFactory();

    private JpaUtil() {
    }

    private static EntityManagerFactory buildEntityManagerFactory() {
        Dotenv dotenv = Dotenv.load();

        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", dotenv.get("DB_URL"));
        props.put("jakarta.persistence.jdbc.user", dotenv.get("DB_USER"));
        props.put("jakarta.persistence.jdbc.password", dotenv.get("DB_PASSWORD"));
        props.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");

        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, props);
    }

    public static EntityManager createEntityManager() {
        return EMF.createEntityManager();
    }

    public static void shutdown() {
        if (EMF.isOpen()) {
            EMF.close();
        }
    }
}
