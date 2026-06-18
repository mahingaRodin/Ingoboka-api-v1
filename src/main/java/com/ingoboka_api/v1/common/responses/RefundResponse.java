package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefundResponse {
    UUID id;
    UUID paymentId;
    BigDecimal amount;
    String reason;
    RefundStatus status;
    Instant createdAt;
}
