package com.ingoboka_api.v1.document.models;

import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import com.ingoboka_api.v1.common.enums.MalwareScanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_registry")
public class DocumentRegistry {

    @Id
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 64)
    private DocumentEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(nullable = false, length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "malware_scan_status", nullable = false, length = 32)
    private MalwareScanStatus malwareScanStatus = MalwareScanStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_classification", nullable = false, length = 32)
    private DocumentAccessClassification accessClassification = DocumentAccessClassification.INTERNAL;

    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
