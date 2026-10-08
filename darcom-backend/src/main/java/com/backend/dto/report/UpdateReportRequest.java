package com.backend.dto.report;

import com.backend.domain.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

/**
 * PATCH /admin/reports/{id} body. Any status accepted — the spec defines no
 * transition machine for reports, so none is invented here.
 */
public class UpdateReportRequest {

    @NotNull
    private ReportStatus status;

    private String adminNotes;

    public UpdateReportRequest() {
    }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
