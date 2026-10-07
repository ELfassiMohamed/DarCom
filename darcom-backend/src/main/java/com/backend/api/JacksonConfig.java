package com.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Gives Jersey's Jackson provider an ObjectMapper that can actually serialize
 * this project's DTOs. Two settings, both load-bearing:
 *
 * 1. JavaTimeModule — response DTOs carry java.time.Instant (createdAt,
 *    updatedAt, expiresAt on nearly every future DTO). Without the module,
 *    serializing any of them fails the whole response.
 * 2. WRITE_DATES_AS_TIMESTAMPS disabled — Instants render as ISO-8601 strings
 *    ("2026-10-06T17:12:22Z"), the same shape Instant.toString() already gives
 *    the error envelope's timestamp field, instead of numeric arrays.
 *
 * DEPLOY-DRIVEN ADDITION (not in TASK-06's manifest): without this file,
 * POST /auth/register persists the user and then fails its own 201 response
 * (proven live: the duplicate-register 409 that followed proved the row
 * existed). The alternative — String-ifying every Instant in every DTO —
 * would corrupt all response shapes task after task. Registered explicitly
 * in RestApplication, per this project's no-scanning convention.
 */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JacksonConfig() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
