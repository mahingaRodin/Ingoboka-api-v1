package com.ingoboka_api.v1.document.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentUrlBuilderTest {

    @Test
    void buildsDocumentContentUrl() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertThat(DocumentUrlBuilder.documentContentUrl(id))
                .isEqualTo("/api/v1/documents/11111111-1111-1111-1111-111111111111/content");
    }

    @Test
    void buildsClaimDocumentContentUrl() {
        UUID claimId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID docId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        assertThat(DocumentUrlBuilder.claimDocumentContentUrl(claimId, docId))
                .isEqualTo(
                        "/api/v1/claims/22222222-2222-2222-2222-222222222222/documents/33333333-3333-3333-3333-333333333333/content");
    }

    @Test
    void detectsApiContentUrls() {
        assertThat(DocumentUrlBuilder.isApiContentUrl("/api/v1/documents/abc/content"))
                .isTrue();
        assertThat(DocumentUrlBuilder.isApiContentUrl(
                        "/api/v1/claims/abc/documents/def/content"))
                .isTrue();
        assertThat(DocumentUrlBuilder.isApiContentUrl("http://minio:9000/bucket/key"))
                .isFalse();
    }

    @Test
    void detectsMinioUrls() {
        assertThat(DocumentUrlBuilder.isMinioOrInternalStorageUrl(
                        "http://minio:9000/ingoboka-documents/docs/u/CLAIM_EVIDENCE-x.pdf?X-Amz-Signature=abc"))
                .isTrue();
        assertThat(DocumentUrlBuilder.isMinioOrInternalStorageUrl(
                        "/api/v1/claims/a/documents/b/content"))
                .isFalse();
    }

    @Test
    void resolvesClaimEvidenceRegistryDocumentToClaimProxy() {
        UUID claimId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID docId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        assertThat(DocumentUrlBuilder.resolveStoredDocumentContentUrl(
                        docId, DocumentEntityType.CLAIM, claimId, "CLAIM_EVIDENCE"))
                .isEqualTo(DocumentUrlBuilder.claimDocumentContentUrl(claimId, docId));
    }

    @Test
    void sanitizesMinioUrlsToApiProxy() {
        String apiUrl = "/api/v1/documents/abc/content";
        assertThat(DocumentUrlBuilder.sanitizeClientUrl(
                        "http://minio:9000/ingoboka-documents/docs/x.pdf?X-Amz-Signature=abc", apiUrl))
                .isEqualTo(apiUrl);
    }
}
