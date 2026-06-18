package com.ingoboka_api.v1.audit.controllers;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.requests.ResolveDataSubjectRequest;
import com.ingoboka_api.v1.common.requests.SubmitDataSubjectRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.AuditLogResponse;
import com.ingoboka_api.v1.common.responses.DataSubjectRequestResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/audit", "/api/v1/audit"})
@RequiredArgsConstructor
@Tag(name = "Audit & Compliance", description = "Immutable audit trail and compliance evidence")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditComplianceService auditComplianceService;

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'COMPLIANCE_AUDITOR', 'PARTNER_ADMIN')")
    @Operation(summary = "List audit logs", description = "Paginated immutable activity trail")
    public ApiResponse<PageResponse<AuditLogResponse>> listLogs(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        PageResponse<AuditLogResponse> data =
                SecurityUtils.currentUser().hasRole(RoleCodes.PLATFORM_ADMIN)
                        ? auditComplianceService.listAuditLogs(page, size)
                        : auditComplianceService.listTenantAuditLogs(page, size);
        return ApiResponse.ok("Audit logs retrieved", data);
    }

    @PostMapping("/data-subject-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit GDPR-style data subject request")
    public ApiResponse<DataSubjectRequestResponse> submitDataSubjectRequest(
            @Valid @RequestBody SubmitDataSubjectRequest request) {
        return ApiResponse.ok("Request submitted", auditComplianceService.submitDataSubjectRequest(request));
    }

    @GetMapping("/data-subject-requests/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my data subject requests (paginated)")
    public ApiResponse<PageResponse<DataSubjectRequestResponse>> listMyDataSubjectRequests(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                "Requests retrieved", auditComplianceService.listMyDataSubjectRequests(page, size));
    }

    @GetMapping("/data-subject-requests")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'COMPLIANCE_AUDITOR')")
    @Operation(summary = "List all data subject requests (paginated)")
    public ApiResponse<PageResponse<DataSubjectRequestResponse>> listAllDataSubjectRequests(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                "Requests retrieved", auditComplianceService.listAllDataSubjectRequests(page, size));
    }

    @PostMapping("/data-subject-requests/resolve")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'COMPLIANCE_AUDITOR')")
    @Operation(summary = "Resolve data subject request")
    public ApiResponse<DataSubjectRequestResponse> resolveDataSubjectRequest(
            @Valid @RequestBody ResolveDataSubjectRequest request) {
        return ApiResponse.ok("Request resolved", auditComplianceService.resolveDataSubjectRequest(request));
    }
}
