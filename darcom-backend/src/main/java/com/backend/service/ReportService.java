package com.backend.service;

import com.backend.domain.Listing;
import com.backend.domain.Report;
import com.backend.domain.User;
import com.backend.domain.enums.ReportReason;
import com.backend.domain.enums.ReportStatus;
import com.backend.domain.enums.UserRole;
import com.backend.exception.NotFoundException;
import com.backend.repository.ListingRepository;
import com.backend.repository.Pageable;
import com.backend.repository.ReportRepository;
import com.backend.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReportService {

    /** POST /reports — reporter is any authenticated caller; reported host must actually be a host. */
    public Report file(User reporter, UUID reportedHostId, UUID listingId,
                       ReportReason reason, String details) {
        return TransactionRunner.call(em -> {
            User reportedHost = new UserRepository(em).findById(reportedHostId)
                    .orElseThrow(() -> new NotFoundException("HOST_NOT_FOUND", "Reported host not found"));
            if (reportedHost.getRole() != UserRole.HOST) {
                throw new NotFoundException("HOST_NOT_FOUND", "Reported user is not a host");
            }

            Listing listing = null;
            if (listingId != null) {
                listing = new ListingRepository(em).findById(listingId)
                        .orElseThrow(() -> new NotFoundException("LISTING_NOT_FOUND", "Listing not found"));
            }

            Report report = new Report();
            report.setReporter(reporter);
            report.setReportedHost(reportedHost);
            report.setListing(listing);
            report.setReason(reason);
            report.setDetails(details);
            report.setStatus(ReportStatus.OPEN);
            return new ReportRepository(em).create(report);
        });
    }

    /** GET /admin/reports — status optional. */
    public PagedResult<Report> getAll(ReportStatus status, Pageable pageable) {
        return TransactionRunner.call(em -> {
            ReportRepository repo = new ReportRepository(em);
            List<Report> items = repo.findByStatus(status, pageable);
            long totalItems = repo.countByStatus(status);
            return new PagedResult<>(items, totalItems);
        });
    }

    /** GET /admin/reports/{id} */
    public Report getById(UUID reportId) {
        return TransactionRunner.call(em -> new ReportRepository(em).findByIdWithDetails(reportId)
                .orElseThrow(() -> new NotFoundException("REPORT_NOT_FOUND", "Report not found")));
    }

    /**
     * PATCH /admin/reports/{id} — any status accepted (the spec defines no
     * transition machine for reports; inventing one would be speculation),
     * reviewedAt stamped on every review.
     */
    public Report review(UUID reportId, ReportStatus status, String adminNotes) {
        return TransactionRunner.call(em -> {
            ReportRepository repo = new ReportRepository(em);
            Report report = repo.findByIdWithDetails(reportId)
                    .orElseThrow(() -> new NotFoundException("REPORT_NOT_FOUND", "Report not found"));
            report.setStatus(status);
            report.setAdminNotes(adminNotes);
            report.setReviewedAt(Instant.now());
            return repo.update(report);
        });
    }
}
