package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentResponse {
    UUID id;
    UUID policyId;
    BigDecimal amount;
    String currency;
    PaymentStatus status;
    String provider;
    String providerReference;
    Instant initiatedAt;
    Instant completedAt;
}
