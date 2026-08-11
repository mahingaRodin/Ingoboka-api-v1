package com.ingoboka_api.v1.document.util;

import java.util.UUID;

/** Builds browser-safe API proxy paths for stored documents (no direct MinIO hostnames). */
public final class DocumentUrlBuilder {

    public static final String DOCUMENT_CONTENT_PREFIX = "/api/v1/documents/";
    public static final String DOCUMENT_CONTENT_SUFFIX = "/content";
    public static final String CLAIM_DOCUMENT_CONTENT_PREFIX = "/api/v1/claims/";

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
}
