package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ReceiptType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReceiptResponse {
    UUID id;
    UUID policyId;
    UUID paymentId;
    UUID refundId;
    String receiptNumber;
    ReceiptType receiptType;
    BigDecimal amount;
    String currency;
    Instant issuedAt;
}
