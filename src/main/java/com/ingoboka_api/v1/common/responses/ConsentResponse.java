package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ConsentType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConsentResponse {
    UUID id;
    ConsentType consentType;
    String version;
    boolean granted;
    Instant grantedAt;
    Instant revokedAt;
}
