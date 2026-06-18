package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.PremiumFrequency;
import com.ingoboka_api.v1.common.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductPlanResponse {
    UUID id;
    UUID productId;
    String code;
    String name;
    String description;
    BigDecimal premiumAmount;
    PremiumFrequency premiumFrequency;
    Integer waitingPeriodDays;
    ProductStatus status;
    List<PlanBenefitResponse> benefits;
    List<PlanExclusionResponse> exclusions;
    EligibilityRuleResponse eligibility;
    Instant createdAt;

    @Value
    @Builder
    public static class PlanBenefitResponse {
        UUID id;
        String title;
        String description;
        BigDecimal coverageLimit;
        Integer sortOrder;
    }

    @Value
    @Builder
    public static class PlanExclusionResponse {
        UUID id;
        String title;
        String description;
        Integer sortOrder;
    }

    @Value
    @Builder
    public static class EligibilityRuleResponse {
        UUID id;
        Integer minAge;
        Integer maxAge;
    }
}
