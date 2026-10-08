package com.backend.service;

import com.backend.domain.Listing;
import com.backend.domain.Message;
import com.backend.domain.User;

/**
 * One conversation row: the counterpart, the listing, its latest message, and
 * how many of the group's messages are still unread by the caller. Service-owned
 * carrier (AuthResult precedent) — the resource maps it to ConversationSummary.
 */
public record Conversation(User counterpart, Listing listing, Message latestMessage, long unreadCount) {
}
