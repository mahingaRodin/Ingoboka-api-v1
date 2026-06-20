package com.ingoboka_api.v1.common.responses;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductDetailResponse {
    ProductResponse product;
    List<ProductPlanResponse> plans;
    List<FaqItem> faq;
    List<ClaimStepItem> claimSteps;
    String currency;

    @Value
    @Builder
    public static class FaqItem {
        String question;
        String answer;
        int sortOrder;
    }

    @Value
    @Builder
    public static class ClaimStepItem {
        int step;
        String title;
        String description;
    }
}
