package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.BillStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BillResponse {
    UUID id;
    UUID policyId;
    String billNumber;
    BigDecimal amount;
    BigDecimal amountPaid;
    String currency;
    BillStatus status;
    LocalDate dueDate;
    Instant issuedAt;
}
