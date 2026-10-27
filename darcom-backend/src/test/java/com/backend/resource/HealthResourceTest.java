package com.backend.resource;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HealthResource")
class HealthResourceTest {

    @Nested
    @DisplayName("GET /health")
    class GetHealth {

        @Test
        @DisplayName("should return 200 with status UP")
        void shouldReturn200_whenHealthEndpointIsCalled() {
            com.backend.resource.HealthResource resource =
                    new com.backend.resource.HealthResource();

            Response response = resource.health();

            assertEquals(200, response.getStatus());
        }
    }
}