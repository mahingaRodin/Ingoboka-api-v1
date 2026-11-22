package com.ingoboka_api.v1.document.impls;

import com.ingoboka_api.v1.common.config.MinioProperties;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.document.model.StoredObject;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import com.ingoboka_api.v1.document.util.DocumentUrlBuilder;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioDocumentStorageService implements DocumentStorageService {

    private final MinioProperties storageProperties;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    void init() {
        if (!storageProperties.isEnabled()) {
            log.warn("S3 document storage is disabled — object storage operations will fail at runtime");
            return;
        }

        Region region = Region.of(storageProperties.getRegion());

        s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        s3Presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        ensureBucketExists();
        log.info("S3 storage initialized — bucket: {}, region: {}",
                storageProperties.getBucket(), storageProperties.getRegion());
    }

    private void ensureBucketExists() {
        String bucket = storageProperties.getBucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.debug("S3 bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket '{}' not found — creating it", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created S3 bucket '{}'", bucket);
        } catch (Exception e) {
            // Bucket may exist in another region or access is denied — log and continue.
            // A hard failure here would prevent the app from starting even when S3 is reachable.
            log.warn("Could not verify S3 bucket '{}': {} — proceeding anyway", bucket, e.getMessage());
        }
    }

    @Override
    public String upload(String objectKey, InputStream inputStream, long size, String contentType) {
        ensureClient();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .key(objectKey)
                            .contentType(contentType)
                            .contentLength(size)
                            .build(),
                    RequestBody.fromInputStream(inputStream, size));
            return objectKey;
        } catch (Exception ex) {
            log.error("S3 upload failed for key '{}': {}", objectKey, ex.getMessage(), ex);
            throw new BusinessException("Failed to upload document: " + ex.getMessage());
        }
    }

    @Override
    public String presignedDownloadUrl(String objectKey) {
        ensureClient();
        try {
            Duration expiry = Duration.ofMinutes(storageProperties.getPresignedExpiryMinutes());

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .key(objectKey)
                            .build())
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest)
                    .url()
                    .toString();

            // If a CloudFront / custom public base URL is configured, rewrite the host.
            if (storageProperties.hasPublicBaseUrl()) {
                url = rewriteHost(url, storageProperties.getPublicBaseUrl());
            }

            return url;
        } catch (Exception ex) {
            log.error("Failed to generate S3 presigned download URL for '{}': {}", objectKey, ex.getMessage(), ex);
            throw new BusinessException("Failed to generate download URL");
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
            Duration expiry = Duration.ofMinutes(storageProperties.getPresignedExpiryMinutes());

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .putObjectRequest(PutObjectRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .key(objectKey)
                            .contentType(contentType)
                            .build())
                    .build();

            return s3Presigner.presignPutObject(presignRequest)
                    .url()
                    .toString();
        } catch (Exception ex) {
            log.error("Failed to generate S3 presigned upload URL for '{}': {}", objectKey, ex.getMessage(), ex);
            throw new BusinessException("Failed to generate upload URL");
        }
    }

    @Override
    public StoredObject open(String objectKey) {
        ensureClient();
        try {
            HeadObjectResponse head = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .key(objectKey)
                            .build());

            InputStream stream = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .key(objectKey)
                            .build());

            String contentType = head.contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return new StoredObject(stream, contentType, head.contentLength());
        } catch (NoSuchKeyException ex) {
            throw new BusinessException("Document not found: " + objectKey);
        } catch (Exception ex) {
            log.error("Failed to open S3 object '{}': {}", objectKey, ex.getMessage(), ex);
            throw new BusinessException("Failed to open document: " + ex.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        ensureClient();
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .key(objectKey)
                            .build());
        } catch (Exception ex) {
            log.error("Failed to delete S3 object '{}': {}", objectKey, ex.getMessage(), ex);
            throw new BusinessException("Failed to delete document");
        }
    }

    @Override
    public boolean exists(String objectKey) {
        ensureClient();
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .key(objectKey)
                            .build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (Exception ex) {
            log.warn("S3 exists check failed for '{}': {}", objectKey, ex.getMessage());
            return false;
        }
    }

    private void ensureClient() {
        if (s3Client == null) {
            throw new BusinessException("Object storage is not available (S3 client not initialized)");
        }
    }

    /**
     * Rewrites the host of an S3 presigned URL to use a custom CDN base URL.
     * Example: replaces "https://my-bucket.s3.amazonaws.com/key?..."
     *          with     "https://cdn.example.com/key?..."
     */
    private static String rewriteHost(String presignedUrl, String publicBaseUrl) {
        try {
            URI original = URI.create(presignedUrl);
            String base = publicBaseUrl.endsWith("/")
                    ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                    : publicBaseUrl;
            String path = original.getRawPath();
            String query = original.getRawQuery();
            return base + path + (query != null ? "?" + query : "");
        } catch (Exception e) {
            log.warn("Failed to rewrite presigned URL host — returning original URL");
            return presignedUrl;
        }
    }
}
