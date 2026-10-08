package com.backend.dto.message;

import com.backend.service.Conversation;

import java.time.Instant;
import java.util.UUID;

/** One GET /messages/conversations row, spec §8 shape. Assembled from the service carrier — no entity crosses into it. */
public record ConversationSummary(
        UUID listingId,
        String listingTitle,
        ParticipantSummary counterpart,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount
) {
    public static ConversationSummary from(Conversation conversation) {
        return new ConversationSummary(
                conversation.listing().getId(),
                conversation.listing().getTitle(),
                ParticipantSummary.from(conversation.counterpart()),
                conversation.latestMessage().getBody(),
                conversation.latestMessage().getSentAt(),
                conversation.unreadCount()
        );
    }
}
