package com.backend.api;

import com.backend.api.security.AuthenticationFilter;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/api/v1")
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
        register(UserResource.class);
        register(HealthResource.class);
    }
}
