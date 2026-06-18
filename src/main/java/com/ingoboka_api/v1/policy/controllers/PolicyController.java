package com.ingoboka_api.v1.policy.controllers;

import com.ingoboka_api.v1.common.requests.AttachPolicyDocumentRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PolicyResponse;
import com.ingoboka_api.v1.common.responses.PolicyVerificationResponse;
import com.ingoboka_api.v1.common.responses.PremiumScheduleResponse;
import com.ingoboka_api.v1.common.responses.PremiumScheduleResponse;
import com.ingoboka_api.v1.policy.services.PolicyLifecycleService;
import com.ingoboka_api.v1.policy.services.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/policies", "/api/v1/policies"})
@RequiredArgsConstructor
@Tag(name = "Policy Management", description = "Issued policies, members, documents, and QR verification")
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyLifecycleService policyLifecycleService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "List my policies")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<PolicyResponse>> listMyPolicies(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Policies retrieved", policyService.listMyPolicies(page, size));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN', 'CLAIMS_OFFICER')")
    @Operation(summary = "List tenant policies")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<PolicyResponse>> listTenantPolicies(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Policies retrieved", policyService.listTenantPolicies(page, size));
    }

    @GetMapping("/verify/{token}")
    @Operation(summary = "Verify policy by QR token", description = "Public endpoint for minimum policy verification data")
    public ApiResponse<PolicyVerificationResponse> verifyPolicy(@PathVariable String token) {
        return ApiResponse.ok("Policy verification result", policyService.verifyByQrToken(token));
    }

    @GetMapping("/{policyId}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'UNDERWRITER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN', 'CLAIMS_OFFICER')")
    @Operation(summary = "Get policy")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PolicyResponse> getPolicy(@PathVariable UUID policyId) {
        return ApiResponse.ok("Policy retrieved", policyService.getPolicy(policyId));
    }

    @PostMapping("/{policyId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CITIZEN', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Attach policy document metadata")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> attachDocument(
            @PathVariable UUID policyId, @Valid @RequestBody AttachPolicyDocumentRequest request) {
        policyService.attachDocument(policyId, request);
        return ApiResponse.ok("Document attached", null);
    }

    @GetMapping("/{policyId}/premium-schedules")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List premium schedules for policy")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<PremiumScheduleResponse>> listPremiumSchedules(
            @PathVariable UUID policyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Premium schedules retrieved", policyService.listPremiumSchedules(policyId, page, size));
    }

    @PostMapping("/{policyId}/renew")
    @PreAuthorize("hasAnyRole('CITIZEN', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Initiate policy renewal")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PolicyResponse> renew(@PathVariable UUID policyId) {
        return ApiResponse.ok("Renewal initiated", policyLifecycleService.renewPolicy(policyId));
    }
}
