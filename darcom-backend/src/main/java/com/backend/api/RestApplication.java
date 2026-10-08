package com.backend.api;

import com.backend.api.security.AuthenticationFilter;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import jakarta.ws.rs.ApplicationPath;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@ApplicationPath("/api/v1")
@OpenAPIDefinition(
        info = @Info(
                title = "DarCom API",
                version = "1.0.0",
                description = "Co-Hosting Platform MVP — 30 endpoints"
        ),
        security = @SecurityRequirement(name = "bearer")
)
@SecurityScheme(
        name = "bearer",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class RestApplication extends ResourceConfig {

    public RestApplication() {
        register(AuthenticationFilter.class);
        register(ApiExceptionMapper.class);
        register(GenericExceptionMapper.class);
        register(ValidationExceptionMapper.class);
        register(RolesAllowedDynamicFeature.class);
        register(JacksonConfig.class);
        register(AuthResource.class);
        register(ListingResource.class);
        register(BookingResource.class);
        register(ListingBookingResource.class);
        register(MessageResource.class);
        register(ListingMessageResource.class);
        register(ReportResource.class);
        register(AdminReportResource.class);
        register(AdminUserResource.class);
        register(UserResource.class);
        register(HealthResource.class);
        register(io.swagger.v3.jaxrs2.integration.resources.OpenApiResource.class);
    }
}
