package com.backend.repository;

import com.backend.domain.Listing;
import com.backend.domain.Message;
import com.backend.domain.User;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

public class MessageRepository extends GenericRepository<Message> {

    public MessageRepository(EntityManager em) {
        super(em, Message.class);
    }

    /**
     * The thread between two specific participants on one listing,
     * chronological. Matches either direction — sender/receiver can be
     * either side of the pair, since who sent a given message varies within
     * the same thread. Parties fetch-joined (all to-one) so DTO mapping stays
     * safe after the transaction closes.
     */
    public List<Message> findThread(Listing listing, User userA, User userB, Pageable pageable) {
        return em.createQuery(
                "SELECT m FROM Message m JOIN FETCH m.listing JOIN FETCH m.sender JOIN FETCH m.receiver " +
                "WHERE m.listing = :listing " +
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

    /** Count side of findThread, for the envelope. */
    public long countThread(Listing listing, User userA, User userB) {
        return em.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.listing = :listing " +
                "AND ((m.sender = :userA AND m.receiver = :userB) " +
                "  OR (m.sender = :userB AND m.receiver = :userA))",
                Long.class)
                .setParameter("listing", listing)
                .setParameter("userA", userA)
                .setParameter("userB", userB)
                .getSingleResult();
    }

    /** Distinct counterpart ids sharing any thread with user on this listing. Empty when no thread exists. */
    public List<UUID> findCounterpartIds(Listing listing, User user) {
        return em.createQuery(
                "SELECT DISTINCT CASE WHEN m.sender = :user THEN m.receiver.id ELSE m.sender.id END " +
                "FROM Message m WHERE m.listing = :listing " +
                "AND (m.sender = :user OR m.receiver = :user)",
                UUID.class)
                .setParameter("listing", listing)
                .setParameter("user", user)
                .getResultList();
    }

    /**
     * Exactly one row per (listing, counterpart) group: its latest message
     * (sentAt DESC, id DESC tiebreak — a total order, so exactly one maximum
     * exists per group). Paginated and ordered directly — no GROUP BY, no CASE,
     * which Postgres cannot reconcile when the CASE holds bind parameters
     * (verified live: "must appear in the GROUP BY clause" on the grouped
     * formulation). Parties fetched for DTO mapping.
     */
    public List<Message> findLatestPerGroup(User caller, Pageable pageable) {
        return em.createQuery(
                "SELECT m FROM Message m JOIN FETCH m.listing JOIN FETCH m.sender JOIN FETCH m.receiver " +
                "WHERE (m.sender = :caller OR m.receiver = :caller) " +
                "AND NOT EXISTS (" +
                "SELECT 1 FROM Message n WHERE n.listing = m.listing " +
                "AND ((n.sender = m.sender AND n.receiver = m.receiver) " +
                "  OR (n.sender = m.receiver AND n.receiver = m.sender)) " +
                "AND (n.sentAt > m.sentAt OR (n.sentAt = m.sentAt AND n.id > m.id))) " +
                "ORDER BY m.sentAt DESC, m.id DESC",
                Message.class)
                .setParameter("caller", caller)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    /** Group count for the envelope: rows with no later in-group message — same determinism as above. */
    public long countConversations(User caller) {
        return em.createQuery(
                "SELECT COUNT(m) FROM Message m " +
                "WHERE (m.sender = :caller OR m.receiver = :caller) " +
                "AND NOT EXISTS (" +
                "SELECT 1 FROM Message n WHERE n.listing = m.listing " +
                "AND ((n.sender = m.sender AND n.receiver = m.receiver) " +
                "  OR (n.sender = m.receiver AND n.receiver = m.sender)) " +
                "AND (n.sentAt > m.sentAt OR (n.sentAt = m.sentAt AND n.id > m.id)))",
                Long.class)
                .setParameter("caller", caller)
                .getSingleResult();
    }

    /** Unread-to-caller messages in one thread. Point query, no grouping involved. */
    public long countUnread(Listing listing, User userA, User userB, User caller) {
        return em.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.listing = :listing " +
                "AND ((m.sender = :userA AND m.receiver = :userB) " +
                "  OR (m.sender = :userB AND m.receiver = :userA)) " +
                "AND m.receiver = :caller AND m.readAt IS NULL",
                Long.class)
                .setParameter("listing", listing)
                .setParameter("userA", userA)
                .setParameter("userB", userB)
                .setParameter("caller", caller)
                .getSingleResult();
    }
}
