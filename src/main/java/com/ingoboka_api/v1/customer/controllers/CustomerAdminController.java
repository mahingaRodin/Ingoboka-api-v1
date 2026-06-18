package com.ingoboka_api.v1.customer.controllers;

import com.ingoboka_api.v1.common.requests.ReviewKycRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.CitizenProfileResponse;
import com.ingoboka_api.v1.customer.services.CustomerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/customers", "/api/v1/customer", "/api/v1/customers"})
@RequiredArgsConstructor
@Tag(name = "Customer Admin", description = "KYC and compliance operations on citizen profiles")
@SecurityRequirement(name = "bearerAuth")
public class CustomerAdminController {

    private final CustomerProfileService customerProfileService;

    @PatchMapping("/kyc/review")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN', 'UNDERWRITER')")
    @Operation(summary = "Approve or reject citizen KYC")
    public ApiResponse<CitizenProfileResponse> reviewKyc(@Valid @RequestBody ReviewKycRequest request) {
        return ApiResponse.ok("KYC updated", customerProfileService.reviewKyc(request));
    }
}
