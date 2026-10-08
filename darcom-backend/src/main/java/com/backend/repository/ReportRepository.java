package com.backend.repository;

import com.backend.domain.Report;
import com.backend.domain.User;
import com.backend.domain.enums.ReportStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ReportRepository extends GenericRepository<Report> {

    public ReportRepository(EntityManager em) {
        super(em, Report.class);
    }

    /** GET /admin/reports — status is optional. Parties fetch-joined (all to-one) for post-transaction DTO mapping. */
    public List<Report> findByStatus(ReportStatus status, Pageable pageable) {
        String jpql = "SELECT r FROM Report r JOIN FETCH r.reporter JOIN FETCH r.reportedHost"
                + (status != null ? " WHERE r.status = :status" : "")
                + " ORDER BY r.createdAt DESC";

        TypedQuery<Report> query = em.createQuery(jpql, Report.class);
        if (status != null) query.setParameter("status", status);

        return query.setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    /** GET /admin/reports/{id} and PATCH review — reporter/host fetched; listing LEFT-joined (nullable FK). No DISTINCT (to-one only). */
    public Optional<Report> findByIdWithDetails(UUID id) {
        return em.createQuery(
                "SELECT r FROM Report r JOIN FETCH r.reporter JOIN FETCH r.reportedHost " +
                "LEFT JOIN FETCH r.listing WHERE r.id = :id",
                Report.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    /** Total count for GET /admin/reports' pagination envelope, status optional (third occurrence of the count gap pattern). */
    public long countByStatus(ReportStatus status) {
        String jpql = "SELECT COUNT(r) FROM Report r"
                + (status != null ? " WHERE r.status = :status" : "");
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        if (status != null) query.setParameter("status", status);
        return query.getSingleResult();
    }

    /** GET /admin/users/{id} detail: how many reports name this host. */
    public long countByReportedHost(User host) {
        return em.createQuery(
                "SELECT COUNT(r) FROM Report r WHERE r.reportedHost = :host", Long.class)
                .setParameter("host", host)
                .getSingleResult();
    }
}
