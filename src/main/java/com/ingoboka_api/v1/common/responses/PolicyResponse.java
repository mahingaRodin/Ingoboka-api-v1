package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.PolicyMemberType;
import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.common.enums.PremiumFrequency;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PolicyResponse {
    UUID id;
    String policyNumber;
    UUID organizationId;
    UUID applicationId;
    UUID citizenProfileId;
    UUID productPlanId;
    PolicyStatus status;
    BigDecimal premiumAmount;
    PremiumFrequency premiumFrequency;
    LocalDate startDate;
    LocalDate endDate;
    String qrVerificationToken;
    List<PolicyMemberResponse> members;
    String productName;
    String insurerName;
    BigDecimal coverageAmount;
    String currency;
    Instant createdAt;
    Instant updatedAt;

    @Value
    @Builder
    public static class PolicyMemberResponse {
        UUID id;
        PolicyMemberType memberType;
        String fullName;
        String relationship;
    }
}
