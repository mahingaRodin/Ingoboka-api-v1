package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ClaimStatus;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaimStatusHistoryItemResponse {
    ClaimStatus status;
    String label;
    Instant occurredAt;
    String note;
}
