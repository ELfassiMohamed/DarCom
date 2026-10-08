package com.backend.dto.report;

import com.backend.domain.enums.ReportReason;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** POST /reports body. reportedHostId must name an actual host (service 404s otherwise); listingId optional. */
public class CreateReportRequest {

    @NotNull
    private UUID reportedHostId;

    private UUID listingId;

    @NotNull
    private ReportReason reason;

    private String details;

    public CreateReportRequest() {
    }

    public UUID getReportedHostId() { return reportedHostId; }
    public void setReportedHostId(UUID reportedHostId) { this.reportedHostId = reportedHostId; }

    public UUID getListingId() { return listingId; }
    public void setListingId(UUID listingId) { this.listingId = listingId; }

    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
