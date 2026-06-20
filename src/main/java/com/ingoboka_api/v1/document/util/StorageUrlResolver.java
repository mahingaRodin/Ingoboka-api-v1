package com.ingoboka_api.v1.document.util;

import com.ingoboka_api.v1.common.config.MinioProperties;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageUrlResolver {

    private final DocumentStorageService documentStorageService;
    private final MinioProperties minioProperties;

    public String resolveDownloadUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        if (!minioProperties.isEnabled()) {
            return null;
        }
        try {
            return documentStorageService.presignedDownloadUrl(objectKey);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
