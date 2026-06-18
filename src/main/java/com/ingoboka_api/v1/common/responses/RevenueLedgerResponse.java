package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.RevenueEntryType;
import com.ingoboka_api.v1.common.enums.RevenueLedgerStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevenueLedgerResponse {
    UUID id;
    UUID organizationId;
    UUID policyId;
    UUID paymentId;
    RevenueEntryType entryType;
    BigDecimal amount;
    String currency;
    RevenueLedgerStatus status;
    String reference;
    String notes;
    Instant createdAt;
}
