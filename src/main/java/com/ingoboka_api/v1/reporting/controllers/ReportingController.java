package com.ingoboka_api.v1.reporting.controllers;

import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.TenantOverviewResponse;
import com.ingoboka_api.v1.reporting.services.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    "/api/partner/reports",
    "/api/v1/partner/reports",
    "/api/v1/insurer/reports",
    "/api/insurer/reports"
})
@RequiredArgsConstructor
@Tag(name = "Reporting & Analytics", description = "Tenant operational dashboards and summaries")
@SecurityRequirement(name = "bearerAuth")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN', 'COMPLIANCE_AUDITOR')")
    @Operation(summary = "Tenant-scoped operational summary")
    public ApiResponse<TenantOverviewResponse> overview() {
        return ApiResponse.ok("Overview retrieved", reportingService.getTenantOverview());
    }

    @GetMapping("/policies")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN', 'COMPLIANCE_AUDITOR')")
    public ApiResponse<PageResponse<?>> policyReport(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Policy report", reportingService.getPolicyReport(page, size));
    }

    @GetMapping("/claims")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'CLAIMS_OFFICER', 'PLATFORM_ADMIN', 'COMPLIANCE_AUDITOR')")
    public ApiResponse<PageResponse<?>> claimReport(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Claim report", reportingService.getClaimReport(page, size));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    public ApiResponse<PageResponse<?>> paymentReport(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Payment report", reportingService.getPaymentReport(page, size));
    }

    @GetMapping(value = "/export/policies", produces = "text/csv")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<String> exportPolicies() {
        return csvResponse("policies.csv", reportingService.exportPoliciesCsv());
    }

    @GetMapping(value = "/export/claims", produces = "text/csv")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'CLAIMS_OFFICER', 'PLATFORM_ADMIN')")
    public ResponseEntity<String> exportClaims() {
        return csvResponse("claims.csv", reportingService.exportClaimsCsv());
    }

    @GetMapping(value = "/export/payments", produces = "text/csv")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    public ResponseEntity<String> exportPayments() {
        return csvResponse("payments.csv", reportingService.exportPaymentsCsv());
    }

    private ResponseEntity<String> csvResponse(String filename, String csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
