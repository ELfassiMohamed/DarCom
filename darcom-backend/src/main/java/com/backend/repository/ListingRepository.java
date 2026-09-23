package com.backend.repository;

import com.backend.domain.Listing;
import com.backend.domain.User;
import com.backend.domain.enums.ListingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ListingRepository extends GenericRepository<Listing> {

    public ListingRepository(EntityManager em) {
        super(em, Listing.class);
    }

    /** GET /listings/mine — every status; the host sees their HIDDEN/REMOVED ones too. */
    public List<Listing> findByHost(User host, Pageable pageable) {
        return em.createQuery(
                "SELECT l FROM Listing l WHERE l.host = :host ORDER BY l.createdAt DESC",
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

    /**
     * GET /listings — public search. Always restricted to ACTIVE; every other
     * filter is optional. Criteria API because the WHERE clause shape depends
     * on which of the four filters the caller actually passed (§3.4).
     */
    public List<Listing> search(String city, BigDecimal minPrice, BigDecimal maxPrice,
                                 Boolean mealsIncluded, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Listing> cq = cb.createQuery(Listing.class);
        Root<Listing> root = cq.from(Listing.class);

        cq.select(root).where(searchPredicates(cb, root, city, minPrice, maxPrice, mealsIncluded));
        cq.orderBy(cb.desc(root.get("createdAt")));

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
