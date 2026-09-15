package com.gitlearn.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application configuration.
 * All REST endpoints are served under /api/*
 */
@ApplicationPath("/api")
public class ApplicationConfig extends Application {
}
