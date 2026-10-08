package com.backend.dto.message;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * POST body for /listings/{listingId}/messages. toUserId is conditional —
 * required iff the sender is the host (service-enforced, ConflictException)
 * — which no static annotation can express, so the field itself is bare
 * and the rule lives in MessageService.send.
 */
public class SendMessageRequest {

    @NotBlank
    private String body;

    private UUID toUserId;

    public SendMessageRequest() {
    }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public UUID getToUserId() { return toUserId; }
    public void setToUserId(UUID toUserId) { this.toUserId = toUserId; }
}
