package com.backend.dto.message;

import com.backend.domain.Message;

import java.time.Instant;
import java.util.UUID;

/** Full message shape. Safe post-transaction: findThread fetch-joins all parties; send() returns in-memory objects. */
public record MessageResponse(
        UUID id,
        UUID listingId,
        ParticipantSummary sender,
        ParticipantSummary receiver,
        String body,
        Instant sentAt,
        Instant readAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getListing().getId(),
                ParticipantSummary.from(message.getSender()),
                ParticipantSummary.from(message.getReceiver()),
                message.getBody(),
                message.getSentAt(),
                message.getReadAt()
        );
    }
}
