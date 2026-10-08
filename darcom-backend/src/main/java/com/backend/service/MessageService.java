package com.backend.service;

import com.backend.domain.Listing;
import com.backend.domain.Message;
import com.backend.domain.User;
import com.backend.exception.ConflictException;
import com.backend.exception.NotFoundException;
import com.backend.repository.ListingRepository;
import com.backend.repository.MessageRepository;
import com.backend.repository.Pageable;
import com.backend.repository.UserRepository;
import org.hibernate.Hibernate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageService {

    /** POST /listings/{listingId}/messages — sender is host (toUserId required) or any visitor (receiver forced to host). */
    public Message send(UUID listingId, User sender, UUID toUserId, String body) {
        return TransactionRunner.call(em -> {
            Listing listing = new ListingRepository(em).findById(listingId)
                    .orElseThrow(() -> new NotFoundException("LISTING_NOT_FOUND", "Listing not found"));

            User receiver;
            if (listing.getHost().getId().equals(sender.getId())) {
                if (toUserId == null) {
                    throw new ConflictException("TO_USER_REQUIRED", "toUserId is required when the sender is the host");
                }
                receiver = new UserRepository(em).findById(toUserId)
                        .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Recipient not found"));
            } else {
                receiver = listing.getHost();
            }
            // The response maps the receiver name post-transaction: a findById-loaded
            // User is already scalar-complete, but listing.getHost() is an
            // uninitialized proxy — initialize covers both branches uniformly.
            Hibernate.initialize(receiver);

            Message message = new Message();
            message.setListing(listing);
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setBody(body);
            return new MessageRepository(em).create(message);
        });
    }

    /**
     * GET /listings/{listingId}/messages — chronological thread. Access is structural:
     * a visitor's counterpart is forced to the host and a host reads only their own
     * threads, so no response can ever contain a third party's messages. Empty
     * (rather than 403) when no thread exists — empty reveals nothing, and a
     * first-time visitor must be able to open the composer before posting.
     * Reading marks messages addressed to the caller as read, same tx (otherwise
     * unreadCount could never decrease).
     */
    public PagedResult<Message> getThread(UUID listingId, User caller, UUID withUser, Pageable pageable) {
        return TransactionRunner.call(em -> {
            MessageRepository repo = new MessageRepository(em);
            Listing listing = new ListingRepository(em).findById(listingId)
                    .orElseThrow(() -> new NotFoundException("LISTING_NOT_FOUND", "Listing not found"));

            User counterpart;
            if (listing.getHost().getId().equals(caller.getId())) {
                if (withUser != null) {
                    counterpart = new UserRepository(em).findById(withUser)
                            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
                } else {
                    List<UUID> ids = repo.findCounterpartIds(listing, caller);
                    if (ids.size() > 1) {
                        throw new ConflictException("WITH_USER_REQUIRED",
                                "withUser is required: multiple threads on this listing");
                    }
                    if (ids.isEmpty()) {
                        return new PagedResult<>(List.of(), 0);
                    }
                    counterpart = new UserRepository(em).findById(ids.get(0))
                            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
                }
            } else {
                counterpart = listing.getHost();
            }

            List<Message> items = repo.findThread(listing, caller, counterpart, pageable);
            for (Message message : items) {
                if (message.getReceiver().getId().equals(caller.getId()) && message.getReadAt() == null) {
                    message.setReadAt(Instant.now());
                    repo.update(message);
                }
            }
            return new PagedResult<>(items, repo.countThread(listing, caller, counterpart));
        });
    }

    /**
     * GET /messages/conversations — one row per (listing, counterpart) group,
     * most-recent-first. The latest-per-group rows come paginated from a single
     * query; counterpart resolution is plain Java (sender == caller ? receiver
     * : sender — no CASE needed); unread is one indexed point query per group,
     * bounded by page size. Total cost ≈ 2 + N trivial queries, all exact.
     */
    public PagedResult<Conversation> getConversations(User caller, Pageable pageable) {
        return TransactionRunner.call(em -> {
            MessageRepository repo = new MessageRepository(em);
            List<Message> latest = repo.findLatestPerGroup(caller, pageable);
            long totalItems = repo.countConversations(caller);

            List<Conversation> items = new ArrayList<>();
            for (Message message : latest) {
                User counterpart = message.getSender().getId().equals(caller.getId())
                        ? message.getReceiver()
                        : message.getSender();
                long unreadCount = repo.countUnread(message.getListing(), counterpart, caller, caller);
                items.add(new Conversation(counterpart, message.getListing(), message, unreadCount));
            }
            return new PagedResult<>(items, totalItems);
        });
    }
}
