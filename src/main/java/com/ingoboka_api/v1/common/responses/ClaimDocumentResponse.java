package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaimDocumentResponse {
    UUID id;
    UUID claimId;
    String documentType;
    String mimeType;
    Long sizeBytes;
    String fileName;
    /** Authenticated API proxy path — stream via GET with Bearer token. */
    String contentUrl;
    Instant createdAt;
}
