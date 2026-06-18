package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditLogResponse {
    UUID id;
    UUID organizationId;
    UUID actorUserId;
    String actorEmail;
    String action;
    String entityType;
    UUID entityId;
    String correlationId;
    String summary;
    Instant createdAt;
}
