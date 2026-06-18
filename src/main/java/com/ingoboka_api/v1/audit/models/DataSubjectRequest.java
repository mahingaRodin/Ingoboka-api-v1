package com.ingoboka_api.v1.audit.models;

import com.ingoboka_api.v1.common.enums.DataSubjectRequestStatus;
import com.ingoboka_api.v1.common.enums.DataSubjectRequestType;
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
@Table(name = "data_subject_requests")
public class DataSubjectRequest {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 64)
    private DataSubjectRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DataSubjectRequestStatus status = DataSubjectRequestStatus.SUBMITTED;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
