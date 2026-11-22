package com.ingoboka_api.v1.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for AWS S3 document storage.
 * Credentials are NOT configured here — the AWS SDK picks them up automatically
 * from the EC2 instance profile (IAM role) or environment variables in this order:
 *   1. Environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
 *   2. EC2 instance profile / ECS task role (preferred in production)
 *   3. ~/.aws/credentials (local dev only)
 *
 * Prefix kept as "ingoboka.storage.minio" temporarily so existing .properties
 * files do not need to change during the migration step.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ingoboka.storage.minio")
public class MinioProperties {

    /** Set false to disable object storage entirely (dev without S3). */
    private boolean enabled = true;

    /**
     * AWS region where the S3 bucket lives (e.g. us-east-1, af-south-1).
     * Maps to env var: S3_REGION (see application-docker.properties).
     */
    private String region = "us-east-1";

    /** S3 bucket name for document storage. Maps to env var: S3_BUCKET / MINIO_BUCKET. */
    private String bucket = "ingoboka-documents";

    /**
     * How long (minutes) a presigned URL is valid.
     * GET presigned URLs are used for direct browser downloads of product PDFs.
     */
    private int presignedExpiryMinutes = 60;

    /**
     * Optional: override the hostname in presigned URLs (e.g. a CloudFront distribution URL).
     * Leave blank to use the default AWS S3 endpoint.
     */
    private String publicBaseUrl;

    public boolean hasPublicBaseUrl() {
        return publicBaseUrl != null && !publicBaseUrl.isBlank();
    }

    /**
     * With Amazon S3, presigned URLs are always browser-reachable HTTPS URLs.
     * This method is kept for compatibility with callers that previously checked
     * whether a MinIO endpoint was accessible outside Docker.
     */
    public boolean isPublicEndpointBrowserReachable() {
        return enabled;
    }

}
