package com.ingoboka_api.v1.document.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ingoboka_api.v1.common.config.MinioProperties;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorageUrlResolverTest {

    private DocumentStorageService documentStorageService;
    private MinioProperties minioProperties;
    private StorageUrlResolver resolver;

    @BeforeEach
    void setUp() {
        documentStorageService = mock(DocumentStorageService.class);
        minioProperties = new MinioProperties();
        minioProperties.setEnabled(true);
        resolver = new StorageUrlResolver(documentStorageService, minioProperties);
    }

    @Test
    void returnsExternalUrlUnchanged() {
        // Absolute URLs (e.g. already a CDN link) pass through without calling S3
        String url = "https://cdn.example.com/avatar.jpg";

        assertThat(resolver.resolveDownloadUrl(url)).isEqualTo(url);
        verifyNoInteractions(documentStorageService);
    }

    @Test
    void presignsStorageKeyWhenS3Enabled() {
        // S3 presigned URLs are always browser-reachable — URL is generated
        when(documentStorageService.presignedDownloadUrl("users/abc/profile.jpg"))
                .thenReturn("https://my-bucket.s3.us-east-1.amazonaws.com/users/abc/profile.jpg?X-Amz-Signature=abc");

        String resolved = resolver.resolveDownloadUrl("users/abc/profile.jpg");

        assertThat(resolved).contains("amazonaws.com");
    }

    @Test
    void returnsNullWhenS3Disabled() {
        // When S3 is disabled, no URL can be generated
        minioProperties.setEnabled(false);

        assertThat(resolver.resolveDownloadUrl("users/abc/profile.jpg")).isNull();
        verifyNoInteractions(documentStorageService);
    }

    @Test
    void returnsNullForNullObjectKey() {
        assertThat(resolver.resolveDownloadUrl(null)).isNull();
        verifyNoInteractions(documentStorageService);
    }

    @Test
    void returnsNullForClaimEvidenceObjectKeys() {
        // Claim evidence is served via the API proxy, never via presigned URLs
        assertThat(resolver.resolveDownloadUrl("docs/user/CLAIM_EVIDENCE-abc.pdf")).isNull();
        verifyNoInteractions(documentStorageService);
    }
}
