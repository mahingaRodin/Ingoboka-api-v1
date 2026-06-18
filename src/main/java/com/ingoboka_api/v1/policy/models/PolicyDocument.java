package com.ingoboka_api.v1.policy.models;

import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "policy_documents")
public class PolicyDocument {

    @Id
    private UUID id;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

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
    @Column(name = "access_classification", nullable = false, length = 32)
    private DocumentAccessClassification accessClassification = DocumentAccessClassification.CUSTOMER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
