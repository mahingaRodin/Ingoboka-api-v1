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
        String url = "https://cdn.example.com/avatar.jpg";

        assertThat(resolver.resolveDownloadUrl(url)).isEqualTo(url);
        verifyNoInteractions(documentStorageService);
    }

    @Test
    void presignsStorageKeyWhenMinioEnabled() {
        minioProperties.setPublicEndpoint("http://20.1.2.3:9000");
        when(documentStorageService.presignedDownloadUrl("users/abc/profile.jpg"))
                .thenReturn("http://20.1.2.3:9000/ingoboka-documents/users/abc/profile.jpg?X-Amz-Signature=abc");

        String resolved = resolver.resolveDownloadUrl("users/abc/profile.jpg");

        assertThat(resolved).contains("20.1.2.3:9000");
    }

    @Test
    void returnsNullWhenPublicEndpointNotBrowserReachable() {
        minioProperties.setEndpoint("http://minio:9000");
        minioProperties.setPublicEndpoint(null);

        assertThat(resolver.resolveDownloadUrl("users/abc/profile.jpg")).isNull();
        verifyNoInteractions(documentStorageService);
    }

    @Test
    void returnsNullWhenMinioDisabled() {
        minioProperties.setEnabled(false);

        assertThat(resolver.resolveDownloadUrl("users/abc/profile.jpg")).isNull();
        verifyNoInteractions(documentStorageService);
    }
}
