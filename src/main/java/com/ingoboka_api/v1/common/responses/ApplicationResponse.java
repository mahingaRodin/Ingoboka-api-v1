package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.enums.PremiumFrequency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApplicationResponse {
    UUID id;
    String applicationReference;
    UUID quoteId;
    UUID citizenProfileId;
    UUID organizationId;
    UUID productPlanId;
    UUID consentId;
    BigDecimal premiumAmount;
    PremiumFrequency premiumFrequency;
    ApplicationStatus status;
    String decisionReason;
    UUID reviewedBy;
    Instant reviewedAt;
    Instant submittedAt;
    Map<String, String> answers;
}
