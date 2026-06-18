package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.PremiumFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CreateProductPlanRequest {

    @NotBlank(message = "Plan code is required")
    private String code;

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Premium amount is required")
    @DecimalMin(value = "0.01", message = "Premium must be positive")
    private BigDecimal premiumAmount;

    @NotNull(message = "Premium frequency is required")
    private PremiumFrequency premiumFrequency;

    private Integer waitingPeriodDays;

    private List<PlanBenefitRequest> benefits;

    private List<PlanExclusionRequest> exclusions;

    private EligibilityRuleRequest eligibility;

    @Data
    public static class PlanBenefitRequest {
        @NotBlank
        private String title;
        private String description;
        private BigDecimal coverageLimit;
        private Integer sortOrder;
    }

    @Data
    public static class PlanExclusionRequest {
        @NotBlank
        private String title;
        private String description;
        private Integer sortOrder;
    }

    @Data
    public static class EligibilityRuleRequest {
        private Integer minAge;
        private Integer maxAge;
    }
}
