package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ClaimStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaimResponse {
    UUID id;
    String claimNumber;
    UUID policyId;
    UUID organizationId;
    String claimType;
    String description;
    BigDecimal claimedAmount;
    String currency;
    String policyNumber;
    String claimantName;
    ClaimStatus status;
    List<ClaimStatusHistoryItemResponse> statusHistory;
    Instant createdAt;
    Instant updatedAt;
}
