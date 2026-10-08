package com.backend.repository;

import com.backend.domain.Report;
import com.backend.domain.User;
import com.backend.domain.enums.ReportStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class ReportRepository extends GenericRepository<Report> {

    public ReportRepository(EntityManager em) {
        super(em, Report.class);
    }

    /** GET /admin/reports — status is optional. */
    public List<Report> findByStatus(ReportStatus status, Pageable pageable) {
        String jpql = "SELECT r FROM Report r"
                + (status != null ? " WHERE r.status = :status" : "")
                + " ORDER BY r.createdAt DESC";

        TypedQuery<Report> query = em.createQuery(jpql, Report.class);
        if (status != null) query.setParameter("status", status);

        return query.setFirstResult(pageable.offset())
                .setMaxResults(pageable.size())
                .getResultList();
    }

    /** GET /admin/users/{id} detail: how many reports name this host. */
    public long countByReportedHost(User host) {
        return em.createQuery(
                "SELECT COUNT(r) FROM Report r WHERE r.reportedHost = :host", Long.class)
                .setParameter("host", host)
                .getSingleResult();
    }
}
