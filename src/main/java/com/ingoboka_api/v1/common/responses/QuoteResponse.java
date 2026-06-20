package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.PremiumFrequency;
import com.ingoboka_api.v1.common.enums.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QuoteResponse {
    UUID id;
    String quoteReference;
    UUID citizenProfileId;
    UUID organizationId;
    UUID productPlanId;
    String productName;
    String planName;
    BigDecimal premiumAmount;
    PremiumFrequency premiumFrequency;
    Instant validUntil;
    QuoteStatus status;
    Map<String, String> answers;
    Instant createdAt;
}
