package com.backend.repository;

import com.backend.domain.User;
import com.backend.domain.enums.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository extends GenericRepository<User> {

    public UserRepository(EntityManager em) {
        super(em, User.class);
    }

    public Optional<User> findByEmail(String email) {
        return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    /** POST /auth/register's 409 EMAIL_ALREADY_EXISTS check, without hydrating a whole User. */
    public boolean existsByEmail(String email) {
        Long count = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    /** GET /admin/users — role, verified, blocked, search are all optional. */
    public List<User> search(UserRole role, Boolean verified, Boolean blocked, String searchText,
                              Pageable pageable, String sortField, boolean asc) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> root = cq.from(User.class);

        cq.select(root).where(searchPredicates(cb, root, role, verified, blocked, searchText));
        cq.orderBy(asc ? cb.asc(root.get(sortField)) : cb.desc(root.get(sortField)));

        return em.createQuery(cq)
                .setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    /** Backwards-compatible default sort: {@code createdAt,desc}. */
    public List<User> search(UserRole role, Boolean verified, Boolean blocked, String searchText,
                              Pageable pageable) {
        return search(role, verified, blocked, searchText, pageable, "createdAt", false);
    }

    /** Same filters as search() — for the paginated envelope's totalItems. */
    public long countSearch(UserRole role, Boolean verified, Boolean blocked, String searchText) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<User> root = cq.from(User.class);

        cq.select(cb.count(root)).where(searchPredicates(cb, root, role, verified, blocked, searchText));
        return em.createQuery(cq).getSingleResult();
    }

    private Predicate[] searchPredicates(CriteriaBuilder cb, Root<User> root, UserRole role,
                                          Boolean verified, Boolean blocked, String searchText) {
        List<Predicate> predicates = new ArrayList<>();

        if (role != null) predicates.add(cb.equal(root.get("role"), role));
        if (verified != null) predicates.add(cb.equal(root.get("verified"), verified));
        if (blocked != null) predicates.add(cb.equal(root.get("blocked"), blocked));
        if (searchText != null && !searchText.isBlank()) {
            String pattern = "%" + escapeLike(searchText.toLowerCase()) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("email")), pattern, '\\')
            ));
        }
        return predicates.toArray(new Predicate[0]);
    }

    /** Escapes {@code %}, {@code _} and backslash so user input is literal in LIKE (used with ESCAPE '\'). */
    static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
