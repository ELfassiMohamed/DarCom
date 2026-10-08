package com.backend.repository;

import com.backend.domain.Listing;
import com.backend.domain.User;
import com.backend.domain.enums.ListingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ListingRepository extends GenericRepository<Listing> {

    public ListingRepository(EntityManager em) {
        super(em, Listing.class);
    }

    /** GET /listings/mine — every status; the host sees their HIDDEN/REMOVED ones too. host is fetch-joined (to-one: pagination-safe, no row duplication) so list items leave the service with a readable host (TASK-07 §4). */
    public List<Listing> findByHost(User host, Pageable pageable) {
        return em.createQuery(
                "SELECT l FROM Listing l JOIN FETCH l.host WHERE l.host = :host ORDER BY l.createdAt DESC",
                Listing.class)
                .setParameter("host", host)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    /** §10 block side effect: every ACTIVE listing of a host being blocked, to hide. */
    public List<Listing> findByHostAndStatus(User host, ListingStatus status) {
        return em.createQuery(
                "SELECT l FROM Listing l WHERE l.host = :host AND l.status = :status",
                Listing.class)
                .setParameter("host", host)
                .setParameter("status", status)
                .getResultList();
    }

    /** Total count for GET /listings/mine's pagination envelope. */
    public long countByHost(User host) {
        return em.createQuery(
                "SELECT COUNT(l) FROM Listing l WHERE l.host = :host", Long.class)
                .setParameter("host", host)
                .getSingleResult();
    }

    /**
     * Eagerly loads host and photos so both are still safely readable after this
     * transaction closes. host uses an inner JOIN FETCH (always present, never
     * null); photos uses LEFT JOIN FETCH (a listing may have zero). DISTINCT
     * matters here: fetch-joining a collection makes the raw result repeat the
     * parent once per child row — without it, a listing with 3 photos would
     * appear 3 times in the result.
     *
     * activities isn't fetch-joined here. photos is a List — a "bag" to
     * Hibernate — and fetch-joining two collections in one query risks
     * MultipleBagFetchException. Not worth the uncertainty for one extra field;
     * ListingService (§7) initializes activities separately instead.
     */
    public Optional<Listing> findByIdWithDetails(UUID id) {
        return em.createQuery(
                "SELECT DISTINCT l FROM Listing l " +
                "JOIN FETCH l.host " +
                "LEFT JOIN FETCH l.photos " +
                "WHERE l.id = :id",
                Listing.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    /**
     * GET /listings — public search. Always restricted to ACTIVE; every other
     * filter is optional. Criteria API because the WHERE clause shape depends
     * on which of the four filters the caller actually passed (§3.4).
     * sort is "field,direction" (spec §1, e.g. sort=createdAt,desc); unknown or
     * absent values fall back to newest-first — a display preference, not worth
     * a 400. Supported fields: createdAt, pricePerNight.
     */
    public List<Listing> search(String city, BigDecimal minPrice, BigDecimal maxPrice,
                                 Boolean mealsIncluded, String sort, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Listing> cq = cb.createQuery(Listing.class);
        Root<Listing> root = cq.from(Listing.class);
        root.fetch("host", JoinType.INNER); // to-one fetch: pagination-safe (TASK-07 §4); collections stay out (bag-fetch hazard)

        cq.select(root).where(searchPredicates(cb, root, city, minPrice, maxPrice, mealsIncluded));
        cq.orderBy(resolveSort(cb, root, sort));

        return em.createQuery(cq)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    public long countSearch(String city, BigDecimal minPrice, BigDecimal maxPrice, Boolean mealsIncluded) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Listing> root = cq.from(Listing.class);

        cq.select(cb.count(root)).where(searchPredicates(cb, root, city, minPrice, maxPrice, mealsIncluded));
        return em.createQuery(cq).getSingleResult();
    }

    /** "field,direction" sort resolver with newest-first fallback (see search javadoc). */
    private Order resolveSort(CriteriaBuilder cb, Root<Listing> root, String sort) {
        if (sort != null) {
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                boolean asc = parts[1].equalsIgnoreCase("asc");
                if (parts[0].equals("pricePerNight")) {
                    return asc ? cb.asc(root.get("pricePerNight")) : cb.desc(root.get("pricePerNight"));
                }
                if (parts[0].equals("createdAt")) {
                    return asc ? cb.asc(root.get("createdAt")) : cb.desc(root.get("createdAt"));
                }
            }
        }
        return cb.desc(root.get("createdAt"));
    }

    private Predicate[] searchPredicates(CriteriaBuilder cb, Root<Listing> root, String city,
                                           BigDecimal minPrice, BigDecimal maxPrice, Boolean mealsIncluded) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), ListingStatus.ACTIVE));

        if (city != null && !city.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
        }
        if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
        if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
        if (mealsIncluded != null) predicates.add(cb.equal(root.get("mealsIncluded"), mealsIncluded));

        return predicates.toArray(new Predicate[0]);
    }
}
