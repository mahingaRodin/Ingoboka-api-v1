package com.ingoboka_api.v1.platform.controllers;

import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.TenantOverviewResponse;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.reporting.services.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/dashboard", "/api/v1/admin/dashboard"})
@RequiredArgsConstructor
@Tag(name = "Platform Admin", description = "Platform-wide dashboard metrics")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final ReportingService reportingService;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Platform overview dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(
                "Dashboard retrieved",
                Map.of(
                        "organizations", organizationRepository.count(),
                        "users", userRepository.count(),
                        "policies", policyRepository.count()));
    }

    @GetMapping("/tenant-overview")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Reuse tenant overview when platform admin has org context")
    public ApiResponse<TenantOverviewResponse> tenantOverview() {
        return ApiResponse.ok("Overview retrieved", reportingService.getTenantOverview());
    }
}
