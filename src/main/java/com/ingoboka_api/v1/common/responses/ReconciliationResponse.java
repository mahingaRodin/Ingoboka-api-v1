package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ReconciliationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReconciliationResponse {
    UUID id;
    LocalDate reconciliationDate;
    BigDecimal totalPayments;
    BigDecimal totalRefunds;
    BigDecimal netAmount;
    ReconciliationStatus status;
    String notes;
    Instant createdAt;
}
