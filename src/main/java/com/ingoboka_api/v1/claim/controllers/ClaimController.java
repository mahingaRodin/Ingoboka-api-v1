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
import com.ingoboka_api.v1.common.responses.ClaimDocumentResponse;
import com.ingoboka_api.v1.common.responses.ClaimResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ingoboka_api.v1.document.model.DocumentContent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/claims")
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
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Record claim decision")
    public ApiResponse<ClaimResponse> recordDecision(
            @PathVariable UUID claimId, @Valid @RequestBody RecordClaimDecisionRequest request) {
        return ApiResponse.ok("Decision recorded", claimService.recordDecision(claimId, request));
    }

    @PostMapping("/{claimId}/cancel")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Cancel a pending claim")
    public ApiResponse<ClaimResponse> cancelClaim(@PathVariable UUID claimId) {
        return ApiResponse.ok("Claim cancelled", claimService.cancelClaim(claimId));
    }

    @DeleteMapping("/{claimId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Delete a draft claim")
    public ApiResponse<Void> deleteDraftClaim(@PathVariable UUID claimId) {
        claimService.deleteDraftClaim(claimId);
        return ApiResponse.ok("Draft claim deleted", null);
    }

    @PostMapping(value = "/{claimId}/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CITIZEN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR')")
    @Operation(summary = "Attach claim document metadata (legacy — prefer multipart upload)")
    public ApiResponse<ClaimDocumentResponse> attachDocument(
            @PathVariable UUID claimId, @Valid @RequestBody AttachClaimDocumentRequest request) {
        return ApiResponse.ok("Document attached", claimService.attachDocument(claimId, request));
    }

    @PostMapping(value = "/{claimId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CITIZEN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR')")
    @Operation(summary = "Upload claim document files (API proxy to MinIO)")
    public ApiResponse<List<ClaimDocumentResponse>> uploadDocuments(
            @PathVariable UUID claimId, @RequestPart("files") MultipartFile[] files) {
        return ApiResponse.ok("Documents uploaded", claimService.uploadDocuments(claimId, files));
    }

    @GetMapping("/{claimId}/documents")
    @PreAuthorize("hasAnyRole('CITIZEN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "List claim documents with API content URLs")
    public ApiResponse<List<ClaimDocumentResponse>> listDocuments(@PathVariable UUID claimId) {
        return ApiResponse.ok("Documents retrieved", claimService.listDocuments(claimId));
    }

    @GetMapping("/{claimId}/documents/{documentId}/content")
    @PreAuthorize("hasAnyRole('CITIZEN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Stream claim document content (API proxy — no direct MinIO access needed)")
    public ResponseEntity<InputStreamResource> streamDocumentContent(
            @PathVariable UUID claimId, @PathVariable UUID documentId) {
        DocumentContent content = claimService.openDocumentContent(claimId, documentId);
        var object = content.storedObject();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.fileName() + "\"")
                .contentLength(object.size())
                .body(new InputStreamResource(object.stream()));
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
