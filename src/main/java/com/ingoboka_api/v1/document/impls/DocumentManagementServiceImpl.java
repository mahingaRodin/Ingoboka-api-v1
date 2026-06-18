package com.ingoboka_api.v1.document.impls;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import com.ingoboka_api.v1.common.enums.MalwareScanStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.RegisterDocumentRequest;
import com.ingoboka_api.v1.common.responses.DocumentResponse;
import com.ingoboka_api.v1.common.responses.DownloadUrlResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.UploadUrlResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.document.models.DocumentRegistry;
import com.ingoboka_api.v1.document.repositories.DocumentRegistryRepository;
import com.ingoboka_api.v1.document.services.DocumentManagementService;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private final DocumentRegistryRepository documentRegistryRepository;
    private final AuditComplianceService auditComplianceService;
    private final DocumentStorageService documentStorageService;

    @Override
    @Transactional(readOnly = true)
    public UploadUrlResponse requestUploadUrl(String documentType, String mimeType) {
        String objectKey = "docs/"
                + SecurityUtils.currentUser().getUserId() + "/"
                + documentType + "-"
                + UUID.randomUUID() + guessExtension(mimeType);
        return UploadUrlResponse.builder()
                .objectKey(objectKey)
                .uploadUrl(documentStorageService.presignedUploadUrl(objectKey, mimeType))
                .expiresInMinutes(60)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadUrlResponse getDownloadUrl(UUID documentId) {
        DocumentRegistry doc = documentRegistryRepository
                .findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found"));
        assertCanAccess(doc);
        return DownloadUrlResponse.builder()
                .objectKey(doc.getObjectKey())
                .downloadUrl(documentStorageService.presignedDownloadUrl(doc.getObjectKey()))
                .expiresInMinutes(60)
                .build();
    }

    private String guessExtension(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        return switch (mimeType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> "";
        };
    }

    @Override
    @Transactional
    public DocumentResponse registerDocument(RegisterDocumentRequest request) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        Instant now = Instant.now();

        DocumentRegistry doc = new DocumentRegistry();
        doc.setId(UUID.randomUUID());
        doc.setOrganizationId(user.getOrganizationId());
        doc.setOwnerUserId(user.getUserId());
        doc.setEntityType(request.getEntityType());
        doc.setEntityId(request.getEntityId());
        doc.setDocumentType(request.getDocumentType());
        doc.setObjectKey(request.getObjectKey());
        doc.setMimeType(request.getMimeType());
        doc.setSizeBytes(request.getSizeBytes());
        doc.setChecksum(request.getChecksum());
        doc.setMalwareScanStatus(MalwareScanStatus.PENDING);
        doc.setAccessClassification(
                request.getAccessClassification() != null
                        ? request.getAccessClassification()
                        : DocumentAccessClassification.INTERNAL);
        doc.setCreatedAt(now);
        documentRegistryRepository.save(doc);

        auditComplianceService.log(
                "DOCUMENT_REGISTERED", "DOCUMENT", doc.getId(), "Registered document metadata for storage object");
        return toResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId) {
        DocumentRegistry doc = documentRegistryRepository
                .findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found"));
        assertCanAccess(doc);
        return toResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> listByEntity(
            DocumentEntityType entityType, UUID entityId, int page, int size) {
        Page<DocumentRegistry> result = documentRegistryRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                entityType, entityId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> listTenantDocuments(int page, int size) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        Page<DocumentRegistry> result = documentRegistryRepository.findByOrganizationIdOrderByCreatedAtDesc(
                user.getOrganizationId(), PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional
    public DocumentResponse markScannedClean(UUID documentId) {
        if (!SecurityUtils.isPlatformAdmin()) {
            throw new BusinessException("Only platform administrators can update scan status");
        }
        DocumentRegistry doc = documentRegistryRepository
                .findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found"));
        doc.setMalwareScanStatus(MalwareScanStatus.CLEAN);
        documentRegistryRepository.save(doc);
        return toResponse(doc);
    }

    private void assertCanAccess(DocumentRegistry doc) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return;
        }
        if (doc.getOwnerUserId() != null && doc.getOwnerUserId().equals(user.getUserId())) {
            return;
        }
        if (doc.getOrganizationId() != null && doc.getOrganizationId().equals(user.getOrganizationId())) {
            return;
        }
        throw new BusinessException("Access denied to this document");
    }

    private DocumentResponse toResponse(DocumentRegistry doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .organizationId(doc.getOrganizationId())
                .entityType(doc.getEntityType())
                .entityId(doc.getEntityId())
                .documentType(doc.getDocumentType())
                .objectKey(doc.getObjectKey())
                .mimeType(doc.getMimeType())
                .sizeBytes(doc.getSizeBytes())
                .checksum(doc.getChecksum())
                .malwareScanStatus(doc.getMalwareScanStatus())
                .accessClassification(doc.getAccessClassification())
                .retentionUntil(doc.getRetentionUntil())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
