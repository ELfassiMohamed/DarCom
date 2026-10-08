package com.backend.dto.report;

import com.backend.domain.Report;
import com.backend.domain.enums.ReportReason;
import com.backend.domain.enums.ReportStatus;
import com.backend.dto.message.ParticipantSummary;

import java.time.Instant;
import java.util.UUID;

/**
 * Full report shape. Parties are ParticipantSummary (same minimal person-ref as
 * messaging — id + name, no contact data). listingId is nullable by spec;
 * read off the proxy id so the listing row itself never needs loading.
 */
public record ReportResponse(
        UUID id,
        ParticipantSummary reporter,
        ParticipantSummary reportedHost,
        UUID listingId,
        ReportReason reason,
        String details,
        ReportStatus status,
        String adminNotes,
        Instant createdAt,
        Instant reviewedAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                ParticipantSummary.from(report.getReporter()),
                ParticipantSummary.from(report.getReportedHost()),
                report.getListing() == null ? null : report.getListing().getId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getAdminNotes(),
                report.getCreatedAt(),
                report.getReviewedAt()
        );
    }
}
