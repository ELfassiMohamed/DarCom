package com.backend.config;

import jakarta.inject.Singleton;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.backend.security.JwtService;
import com.backend.security.PasswordService;
import com.backend.service.AdminUserService;
import com.backend.service.AuthService;
import com.backend.service.MailService;
import com.backend.service.UserService;

/**
 * JAX-RS bootstrap. Base path {@code /api}; HK2 (jersey-hk2) is the DI mechanism —
 * there is no CDI implementation on the classpath, so "injectable service" here means
 * an HK2-bound singleton. More bindings are added in later phases.
 */
@ApplicationPath("/api")
public class ApiApplication extends ResourceConfig {

    public ApiApplication() {
        packages("com.backend.resource", "com.backend.exception", "com.backend.web", "com.backend.config",
                "com.backend.security");
        register(RolesAllowedDynamicFeature.class);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(AppConfig.class).to(AppConfig.class).in(Singleton.class);
                bind(JwtService.class).to(JwtService.class).in(Singleton.class);
                bind(PasswordService.class).to(PasswordService.class).in(Singleton.class);
                bind(AuthService.class).to(AuthService.class).in(Singleton.class);
                bind(UserService.class).to(UserService.class).in(Singleton.class);
                bind(AdminUserService.class).to(AdminUserService.class).in(Singleton.class);
                bind(MailService.class).to(MailService.class).in(Singleton.class);
            }
        });
    }
}
