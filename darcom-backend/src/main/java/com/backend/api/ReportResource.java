package com.backend.api;

import com.backend.domain.Report;
import com.backend.dto.report.CreateReportRequest;
import com.backend.dto.report.ReportResponse;
import com.backend.service.ReportService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static com.backend.api.ResourceSupport.currentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/reports")
@RolesAllowed("VISITOR")
@Tag(name = "Reports", description = "File and review host reports")
public class ReportResource {

    private final ReportService reportService = new ReportService();

    /** POST /reports — any visitor may file; service resolves host + optional listing. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "File a report against a host")
    @ApiResponse(responseCode = "201", description = "Report opened")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Reported host not found")
    public Response file(@Valid CreateReportRequest request, @Context ContainerRequestContext ctx) {
        Report created = reportService.file(currentUser(ctx), request.getReportedHostId(),
                request.getListingId(), request.getReason(), request.getDetails());
        return Response.status(Response.Status.CREATED).entity(ReportResponse.from(created)).build();
    }
}
