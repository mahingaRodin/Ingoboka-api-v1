package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.PremiumScheduleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PremiumScheduleResponse {
    UUID id;
    UUID policyId;
    LocalDate dueDate;
    BigDecimal amount;
    PremiumScheduleStatus status;
    Instant paidAt;
    UUID paymentId;
}
