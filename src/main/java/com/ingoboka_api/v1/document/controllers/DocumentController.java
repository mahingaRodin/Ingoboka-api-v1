package com.ingoboka_api.v1.document.controllers;

import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import com.ingoboka_api.v1.common.requests.RegisterDocumentRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.DocumentResponse;
import com.ingoboka_api.v1.common.responses.DownloadUrlResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.UploadUrlResponse;
import com.ingoboka_api.v1.document.services.DocumentManagementService;
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
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "Secure document metadata, checksums, and access control")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentManagementService documentManagementService;

    @PostMapping("/upload-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get presigned MinIO upload URL")
    public ApiResponse<UploadUrlResponse> uploadUrl(
            @RequestParam String documentType, @RequestParam String mimeType) {
        return ApiResponse.ok("Upload URL generated", documentManagementService.requestUploadUrl(documentType, mimeType));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register document metadata after upload to object storage")
    public ApiResponse<DocumentResponse> register(@Valid @RequestBody RegisterDocumentRequest request) {
        return ApiResponse.ok("Document registered", documentManagementService.registerDocument(request));
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get document metadata")
    public ApiResponse<DocumentResponse> get(@PathVariable UUID documentId) {
        return ApiResponse.ok("Document retrieved", documentManagementService.getDocument(documentId));
    }

    @GetMapping("/{documentId}/download-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get presigned download URL (RBAC enforced)")
    public ApiResponse<DownloadUrlResponse> downloadUrl(@PathVariable UUID documentId) {
        return ApiResponse.ok("Download URL generated", documentManagementService.getDownloadUrl(documentId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List documents by entity or tenant")
    public ApiResponse<PageResponse<DocumentResponse>> list(
            @RequestParam(required = false) DocumentEntityType entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (entityType != null && entityId != null) {
            return ApiResponse.ok(
                    "Documents retrieved",
                    documentManagementService.listByEntity(entityType, entityId, page, size));
        }
        return ApiResponse.ok(
                "Documents retrieved", documentManagementService.listTenantDocuments(page, size));
    }

    @PostMapping("/{documentId}/scan-clean")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Mark document malware scan as clean")
    public ApiResponse<DocumentResponse> markClean(@PathVariable UUID documentId) {
        return ApiResponse.ok("Scan updated", documentManagementService.markScannedClean(documentId));
    }
}
