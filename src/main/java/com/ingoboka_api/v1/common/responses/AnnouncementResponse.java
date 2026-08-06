package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AnnouncementResponse {
    UUID id;
    String title;
    String body;
    String source;
    UUID organizationId;
    Instant createdAt;
    Instant expiresAt;
}
