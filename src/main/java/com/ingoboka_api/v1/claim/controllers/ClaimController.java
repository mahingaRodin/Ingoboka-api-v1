package com.ingoboka_api.v1.claim.controllers;

import com.ingoboka_api.v1.claim.services.ClaimService;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.requests.AttachClaimDocumentRequest;
import com.ingoboka_api.v1.common.requests.CreateClaimAppealRequest;
import com.ingoboka_api.v1.common.requests.CreateClaimRequest;
import com.ingoboka_api.v1.common.requests.RecordClaimDecisionRequest;
import com.ingoboka_api.v1.common.requests.UpdateClaimStatusRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.ClaimAppealResponse;
import com.ingoboka_api.v1.common.responses.ClaimResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
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
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(name = "Claims & Appeals", description = "Claim intake, workflow, decisions, and appeals")
@SecurityRequirement(name = "bearerAuth")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Create claim draft")
    public ApiResponse<ClaimResponse> createClaim(@Valid @RequestBody CreateClaimRequest request) {
        return ApiResponse.ok("Claim created", claimService.createClaim(request));
    }

    @PostMapping("/{claimId}/submit")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Submit claim")
    public ApiResponse<ClaimResponse> submitClaim(@PathVariable UUID claimId) {
        return ApiResponse.ok("Claim submitted", claimService.submitClaim(claimId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "List my claims")
    public ApiResponse<PageResponse<ClaimResponse>> listMyClaims(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Claims retrieved", claimService.listMyClaims(page, size));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "List tenant claims")
    public ApiResponse<PageResponse<ClaimResponse>> listTenantClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Claims retrieved", claimService.listTenantClaims(status, page, size));
    }

    @GetMapping("/{claimId}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Get claim")
    public ApiResponse<ClaimResponse> getClaim(@PathVariable UUID claimId) {
        return ApiResponse.ok("Claim retrieved", claimService.getClaim(claimId));
    }

    @PatchMapping("/{claimId}/status")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Update claim status")
    public ApiResponse<ClaimResponse> updateStatus(
            @PathVariable UUID claimId, @Valid @RequestBody UpdateClaimStatusRequest request) {
        return ApiResponse.ok("Claim status updated", claimService.updateStatus(claimId, request));
    }

    @PostMapping("/{claimId}/decision")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PLATFORM_ADMIN')")
    @Operation(summary = "Record claim decision")
    public ApiResponse<ClaimResponse> recordDecision(
            @PathVariable UUID claimId, @Valid @RequestBody RecordClaimDecisionRequest request) {
        return ApiResponse.ok("Decision recorded", claimService.recordDecision(claimId, request));
    }

    @PostMapping("/{claimId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CITIZEN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR')")
    @Operation(summary = "Attach claim document metadata")
    public ApiResponse<Void> attachDocument(
            @PathVariable UUID claimId, @Valid @RequestBody AttachClaimDocumentRequest request) {
        claimService.attachDocument(claimId, request);
        return ApiResponse.ok("Document attached", null);
    }

    @PostMapping("/{claimId}/appeals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Open claim appeal")
    public ApiResponse<ClaimAppealResponse> createAppeal(
            @PathVariable UUID claimId, @Valid @RequestBody CreateClaimAppealRequest request) {
        return ApiResponse.ok("Appeal submitted", claimService.createAppeal(claimId, request));
    }
}
