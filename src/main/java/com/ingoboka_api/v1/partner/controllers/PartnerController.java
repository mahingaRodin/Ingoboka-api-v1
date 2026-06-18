package com.ingoboka_api.v1.partner.controllers;

import com.ingoboka_api.v1.common.requests.OnboardPartnerRequest;
import com.ingoboka_api.v1.common.requests.UpdatePartnerRequest;
import com.ingoboka_api.v1.common.requests.UpdatePartnerStatusRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.OnboardPartnerResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PartnerResponse;
import com.ingoboka_api.v1.partner.services.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
@Tag(name = "Partner Management", description = "Insurer and partner tenant onboarding and configuration")
@SecurityRequirement(name = "bearerAuth")
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Onboard partner", description = "Create insurer/partner org, profile, and partner admin with activation email")
    public ApiResponse<OnboardPartnerResponse> onboardPartner(@Valid @RequestBody OnboardPartnerRequest request) {
        return ApiResponse.ok("Partner onboarded successfully", partnerService.onboardPartner(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List partners", description = "List all insurer and partner tenants")
    public ApiResponse<PageResponse<PartnerResponse>> listPartners(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Partners retrieved", partnerService.listPartners(page, size));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Get my partner organization", description = "Partner admin views own tenant profile")
    public ApiResponse<PartnerResponse> getMyPartner() {
        return ApiResponse.ok("Partner retrieved", partnerService.getMyPartner());
    }

    @GetMapping("/{partnerId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "Get partner", description = "Platform admin or owning partner admin")
    public ApiResponse<PartnerResponse> getPartner(@PathVariable UUID partnerId) {
        return ApiResponse.ok("Partner retrieved", partnerService.getPartner(partnerId));
    }

    @PatchMapping("/{partnerId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "Update partner", description = "Update organization name and partner profile")
    public ApiResponse<PartnerResponse> updatePartner(
            @PathVariable UUID partnerId, @Valid @RequestBody UpdatePartnerRequest request) {
        return ApiResponse.ok("Partner updated", partnerService.updatePartner(partnerId, request));
    }

    @PatchMapping("/{partnerId}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update partner status", description = "Suspend or reactivate a tenant")
    public ApiResponse<PartnerResponse> updatePartnerStatus(
            @PathVariable UUID partnerId, @Valid @RequestBody UpdatePartnerStatusRequest request) {
        return ApiResponse.ok("Partner status updated", partnerService.updatePartnerStatus(partnerId, request));
    }
}
