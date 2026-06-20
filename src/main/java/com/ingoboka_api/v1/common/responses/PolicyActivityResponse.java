package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PolicyActivityResponse {
    String type;
    String label;
    Instant occurredAt;
    UUID policyId;
    UUID claimId;
}
