package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.IntegrationAdapterType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IntegrationAdapterResponse {
    UUID id;
    String code;
    IntegrationAdapterType adapterType;
    String name;
    boolean enabled;
    String configJson;
    Instant createdAt;
    Instant updatedAt;
}
