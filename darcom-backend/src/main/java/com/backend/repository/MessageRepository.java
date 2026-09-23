package com.backend.repository;

import com.backend.domain.Listing;
import com.backend.domain.Message;
import com.backend.domain.User;
import jakarta.persistence.EntityManager;

import java.util.List;

public class MessageRepository extends GenericRepository<Message> {

    public MessageRepository(EntityManager em) {
        super(em, Message.class);
    }

    /**
     * The thread between two specific participants on one listing,
     * chronological. Matches either direction — sender/receiver can be
     * either side of the pair, since who sent a given message varies within
     * the same thread.
     */
    public List<Message> findThread(Listing listing, User userA, User userB, Pageable pageable) {
        return em.createQuery(
                "SELECT m FROM Message m WHERE m.listing = :listing " +
                "AND ((m.sender = :userA AND m.receiver = :userB) " +
                "  OR (m.sender = :userB AND m.receiver = :userA)) " +
                "ORDER BY m.sentAt ASC",
                Message.class)
                .setParameter("listing", listing)
                .setParameter("userA", userA)
                .setParameter("userB", userB)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }
}
