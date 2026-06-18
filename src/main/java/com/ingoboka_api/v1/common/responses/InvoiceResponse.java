package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InvoiceResponse {
    UUID id;
    UUID organizationId;
    String invoiceNumber;
    LocalDate periodStart;
    LocalDate periodEnd;
    BigDecimal amount;
    String currency;
    InvoiceStatus status;
    Instant issuedAt;
    Instant createdAt;
}
