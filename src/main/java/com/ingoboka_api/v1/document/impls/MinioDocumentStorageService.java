package com.ingoboka_api.v1.document.impls;

import com.ingoboka_api.v1.common.config.MinioProperties;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import com.ingoboka_api.v1.document.model.StoredObject;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import com.ingoboka_api.v1.document.util.DocumentUrlBuilder;
import io.minio.http.Method;
import io.minio.StatObjectResponse;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioDocumentStorageService implements DocumentStorageService {

    private final MinioProperties minioProperties;
    private MinioClient minioClient;
    /** Separate client for presigned URLs so the host is browser-reachable. */
    private MinioClient presignClient;

    @PostConstruct
    void init() {
        if (!minioProperties.isEnabled()) {
            log.warn("MinIO storage disabled");
            return;
        }
        minioClient = buildClient(minioProperties.getEndpoint());
        String publicEndpoint = minioProperties.getEffectivePublicEndpoint();
        if (minioProperties.isPublicEndpointBrowserReachable()
                && !publicEndpoint.equals(minioProperties.getEndpoint())) {
            presignClient = buildClient(publicEndpoint);
            log.info(
                    "MinIO presigned URLs will use public endpoint {} (internal: {})",
                    publicEndpoint,
                    minioProperties.getEndpoint());
        } else {
            presignClient = minioClient;
            if (!minioProperties.isPublicEndpointBrowserReachable()) {
                log.warn(
                        "MinIO public endpoint {} is not browser-reachable — presigned document URLs may fail in the browser; profile photos and claim uploads are served via the API proxy",
                        publicEndpoint);
            }
        }
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
                log.info("Created MinIO bucket {}", minioProperties.getBucket());
            }
        } catch (Exception ex) {
            log.error("MinIO initialization failed: {}", ex.getMessage());
        }
    }

    private MinioClient buildClient(String endpoint) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Override
    public String upload(String objectKey, InputStream inputStream, long size, String contentType) {
        ensureClient();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception ex) {
            throw new BusinessException("Failed to upload document: " + ex.getMessage());
        }
    }

    @Override
    public String presignedDownloadUrl(String objectKey) {
        ensureClient();
        if (!minioProperties.isPublicEndpointBrowserReachable()) {
            throw new BusinessException(
                    "Presigned download URLs require MINIO_PUBLIC_ENDPOINT; use the API content proxy instead");
        }
        try {
            String url = presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
            if (containsInternalHost(url)) {
                throw new BusinessException(
                        "Presigned download URL is not browser-reachable; configure MINIO_PUBLIC_ENDPOINT");
            }
            return url;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to generate download URL for {}: {}", objectKey, ex.getMessage(), ex);
            throw new BusinessException("Failed to generate download URL");
        }
    }

    @Override
    public StoredObject open(String objectKey) {
        ensureClient();
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
            var response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
            String contentType = stat.contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            return new StoredObject(response, contentType, stat.size());
        } catch (Exception ex) {
            throw new BusinessException("Failed to open document: " + ex.getMessage());
        }
    }

    @Override
    public String presignedUploadUrl(String objectKey, String contentType) {
        ensureClient();
        if (DocumentUrlBuilder.isClaimEvidenceObjectKey(objectKey)) {
            throw new BusinessException(
                    "Presigned upload URLs are not allowed for claim evidence; use POST /api/v1/claims/{claimId}/documents");
        }
        try {
            String url = presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
            if (containsInternalHost(url)) {
                throw new BusinessException(
                        "Presigned upload URL is not browser-reachable; configure MINIO_PUBLIC_ENDPOINT or use the API upload proxy");
            }
            return url;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to generate upload URL for {}: {}", objectKey, ex.getMessage(), ex);
            throw new BusinessException("Failed to generate upload URL");
        }
    }

    @Override
    public void delete(String objectKey) {
        ensureClient();
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("Failed to delete document");
        }
    }

    @Override
    public boolean exists(String objectKey) {
        ensureClient();
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void ensureClient() {
        if (minioClient == null) {
            throw new BusinessException("Object storage is not available");
        }
    }

    private static boolean containsInternalHost(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String lower = url.toLowerCase();
        return lower.contains("://minio:")
                || lower.contains("://minio/")
                || lower.startsWith("http://minio")
                || lower.startsWith("https://minio");
    }
}
