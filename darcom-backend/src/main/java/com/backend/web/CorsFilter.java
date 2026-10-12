package com.backend.web;

import com.backend.config.AppConfig;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/** Minimal CORS: answers preflight with 204 and echoes an allowed {@code Origin} back. */
@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOW_HEADERS = "Authorization, Content-Type";
    private static final String ALLOW_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";

    private final AppConfig config;

    @Inject
    public CorsFilter(AppConfig config) {
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            Response.ResponseBuilder preflight = Response.status(204)
                    .header("Access-Control-Allow-Headers", ALLOW_HEADERS)
                    .header("Access-Control-Allow-Methods", ALLOW_METHODS)
                    .header("Vary", "Origin");
            String origin = request.getHeaderString("Origin");
            if (isAllowed(origin)) {
                preflight.header("Access-Control-Allow-Origin", origin);
            }
            request.abortWith(preflight.build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String origin = request.getHeaderString("Origin");
        if (isAllowed(origin)) {
            response.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
        }
        response.getHeaders().putSingle("Access-Control-Allow-Headers", ALLOW_HEADERS);
        response.getHeaders().putSingle("Access-Control-Allow-Methods", ALLOW_METHODS);
        response.getHeaders().add("Vary", "Origin");
    }

    private boolean isAllowed(String origin) {
        return origin != null && config.getCorsAllowedOrigins().contains(origin);
    }
}
