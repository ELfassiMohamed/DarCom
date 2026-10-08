package com.backend.api;

import com.backend.domain.Report;
import com.backend.domain.enums.ReportStatus;
import com.backend.dto.PagedResponse;
import com.backend.dto.report.ReportResponse;
import com.backend.dto.report.UpdateReportRequest;
import com.backend.repository.Pageable;
import com.backend.service.PagedResult;
import com.backend.service.ReportService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

import static com.backend.api.ResourceSupport.normalizePageable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/admin/reports")
@RolesAllowed("ADMIN")
@Tag(name = "Admin - Reports", description = "Review filed reports")
public class AdminReportResource {

    private final ReportService reportService = new ReportService();

    /** GET /admin/reports — status optional. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List reports, optionally filtered by status")
    @ApiResponse(responseCode = "200", description = "Paginated reports")
    public Response list(@QueryParam("status") ReportStatus status,
                         @QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size) {
        Pageable pageable = normalizePageable(page, size);
        PagedResult<Report> result = reportService.getAll(status, pageable);
        List<ReportResponse> items = result.items().stream().map(ReportResponse::from).toList();
        return Response.ok(new PagedResponse<>(items, pageable.page(), pageable.size(), result.totalItems())).build();
    }

    /** GET /admin/reports/{id} */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get one report with parties")
    @ApiResponse(responseCode = "200", description = "Full report")
    @ApiResponse(responseCode = "404", description = "Report not found")
    public Response getById(@PathParam("id") UUID id) {
        return Response.ok(ReportResponse.from(reportService.getById(id))).build();
    }

    /** PATCH /admin/reports/{id} — any status (no transition machine in spec), reviewedAt stamped. */
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Review a report")
    @ApiResponse(responseCode = "200", description = "Reviewed report")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Report not found")
    public Response review(@PathParam("id") UUID id, @Valid UpdateReportRequest request) {
        Report updated = reportService.review(id, request.getStatus(), request.getAdminNotes());
        return Response.ok(ReportResponse.from(updated)).build();
    }
}
