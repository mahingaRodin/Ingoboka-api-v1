package com.ingoboka_api.v1.document.util;

import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import java.util.UUID;

/** Builds browser-safe API proxy paths for stored documents (no direct MinIO hostnames). */
public final class DocumentUrlBuilder {

    public static final String DOCUMENT_CONTENT_PREFIX = "/api/v1/documents/";
    public static final String DOCUMENT_CONTENT_SUFFIX = "/content";
    public static final String CLAIM_DOCUMENT_CONTENT_PREFIX = "/api/v1/claims/";
    public static final String CLAIM_EVIDENCE_DOCUMENT_TYPE = "CLAIM_EVIDENCE";

    private DocumentUrlBuilder() {}

    public static String documentContentUrl(UUID documentId) {
        return DOCUMENT_CONTENT_PREFIX + documentId + DOCUMENT_CONTENT_SUFFIX;
    }

    public static String claimDocumentContentUrl(UUID claimId, UUID documentId) {
        return CLAIM_DOCUMENT_CONTENT_PREFIX
                + claimId
                + "/documents/"
                + documentId
                + DOCUMENT_CONTENT_SUFFIX;
    }

    public static boolean isApiContentUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return url.contains(DOCUMENT_CONTENT_SUFFIX)
                && (url.contains(DOCUMENT_CONTENT_PREFIX) || url.contains("/documents/"));
    }

    public static boolean isClaimEvidenceDocumentType(String documentType) {
        return documentType != null && CLAIM_EVIDENCE_DOCUMENT_TYPE.equalsIgnoreCase(documentType.trim());
    }

    public static boolean isClaimEvidenceObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }
        return objectKey.contains(CLAIM_EVIDENCE_DOCUMENT_TYPE);
    }

    /** True when the URL points at MinIO or another Docker-internal storage host. */
    public static boolean isMinioOrInternalStorageUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.contains("://minio:")
                || lower.contains("://minio/")
                || lower.startsWith("http://minio")
                || lower.startsWith("https://minio")
                || lower.contains(":9000/ingoboka-documents")
                || lower.contains("://localhost")
                || lower.contains("://127.0.0.1");
    }

    /**
     * Resolves the API content proxy path for a stored document. Claim evidence always uses the
     * claim-scoped proxy, never presigned MinIO URLs.
     */
    public static String resolveStoredDocumentContentUrl(
            UUID documentId, DocumentEntityType entityType, UUID entityId, String documentType) {
        if (isClaimEvidenceDocumentType(documentType)) {
            if (entityType == DocumentEntityType.CLAIM && entityId != null) {
                return claimDocumentContentUrl(entityId, documentId);
            }
            return documentContentUrl(documentId);
        }
        return documentContentUrl(documentId);
    }

    /** Never expose MinIO URLs to clients — fall back to the API proxy when needed. */
    public static String sanitizeClientUrl(String candidate, String apiContentUrl) {
        if (candidate == null || candidate.isBlank() || isMinioOrInternalStorageUrl(candidate)) {
            return apiContentUrl;
        }
        if (isApiContentUrl(candidate)) {
            return candidate;
        }
        return apiContentUrl;
    }
}
