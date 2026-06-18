package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrganizationSettingsResponse {
    UUID organizationId;
    String settingsJson;
    Instant updatedAt;
}
