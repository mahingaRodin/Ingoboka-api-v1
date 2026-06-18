package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import com.ingoboka_api.v1.common.enums.MalwareScanStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentResponse {
    UUID id;
    UUID organizationId;
    DocumentEntityType entityType;
    UUID entityId;
    String documentType;
    String objectKey;
    String mimeType;
    Long sizeBytes;
    String checksum;
    MalwareScanStatus malwareScanStatus;
    DocumentAccessClassification accessClassification;
    LocalDate retentionUntil;
    Instant createdAt;
}
