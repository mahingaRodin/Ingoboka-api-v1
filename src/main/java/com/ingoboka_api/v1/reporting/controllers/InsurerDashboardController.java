package com.ingoboka_api.v1.reporting.controllers;

import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.InsurerDashboardResponse;
import com.ingoboka_api.v1.common.responses.RevenueTrendResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.ProductResponse;
import com.ingoboka_api.v1.product.services.ProductCatalogService;
import com.ingoboka_api.v1.reporting.services.InsurerAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insurer")
@RequiredArgsConstructor
@Tag(name = "Insurer Portal", description = "Tenant-scoped insurer dashboard and analytics")
@SecurityRequirement(name = "bearerAuth")
public class InsurerDashboardController {

    private final InsurerAnalyticsService insurerAnalyticsService;
    private final ProductCatalogService productCatalogService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'UNDERWRITER', 'INSURER_PRODUCT_MANAGER', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    @Operation(summary = "Insurer dashboard", description = "Overview, charts, and geography scoped to the authenticated insurer tenant")
    public ApiResponse<InsurerDashboardResponse> dashboard() {
        return ApiResponse.ok("Dashboard retrieved", insurerAnalyticsService.getDashboard());
    }

    @GetMapping("/revenue/trends")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    @Operation(summary = "Revenue trends", description = "Monthly, weekly, or yearly revenue breakdown for the insurer tenant")
    public ApiResponse<RevenueTrendResponse> revenueTrends(
            @RequestParam(defaultValue = "monthly") String granularity) {
        return ApiResponse.ok("Revenue trends retrieved", insurerAnalyticsService.getRevenueTrends(granularity));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('INSURER_PRODUCT_MANAGER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "List tenant products (insurer alias)")
    public ApiResponse<PageResponse<ProductResponse>> tenantProducts(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Products retrieved", productCatalogService.listTenantProducts(page, size));
    }
}
