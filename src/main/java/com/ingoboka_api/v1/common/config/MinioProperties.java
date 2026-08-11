package com.ingoboka_api.v1.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ingoboka.storage.minio")
public class MinioProperties {
    private boolean enabled = true;
    /** Internal endpoint used by the API container for upload/delete (e.g. http://minio:9000). */
    private String endpoint = "http://localhost:9000";
    /**
     * Browser-reachable endpoint for presigned URLs (e.g. http://YOUR_IP:9000).
     * Falls back to {@link #endpoint} when unset.
     */
    private String publicEndpoint;
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "ingoboka-documents";
    private int presignedExpiryMinutes = 60;

    public String getEffectivePublicEndpoint() {
        if (publicEndpoint != null && !publicEndpoint.isBlank()) {
            return publicEndpoint.trim();
        }
        return endpoint;
    }

    /** True when presigned URLs can be opened directly in a browser (not Docker-internal hostnames). */
    public boolean isPublicEndpointBrowserReachable() {
        String effective = getEffectivePublicEndpoint();
        if (effective == null || effective.isBlank()) {
            return false;
        }
        String lower = effective.toLowerCase();
        return !lower.contains("://minio:")
                && !lower.contains("://minio/")
                && !lower.startsWith("http://minio")
                && !lower.startsWith("https://minio");
    }
}
