package com.ingoboka_api.v1.document.impls;

import com.ingoboka_api.v1.common.config.MinioProperties;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
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

    @PostConstruct
    void init() {
        if (!minioProperties.isEnabled()) {
            log.warn("MinIO storage disabled");
            return;
        }
        minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
            }
        } catch (Exception ex) {
            log.error("MinIO initialization failed: {}", ex.getMessage());
        }
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
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("Failed to generate download URL");
        }
    }

    @Override
    public String presignedUploadUrl(String objectKey, String contentType) {
        ensureClient();
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception ex) {
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
}
