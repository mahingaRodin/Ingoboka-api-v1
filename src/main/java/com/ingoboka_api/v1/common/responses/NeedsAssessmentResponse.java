package com.ingoboka_api.v1.common.responses;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NeedsAssessmentResponse {

    int score;
    String guidance;
    List<String> recommendedCategories;
    List<RecommendedProductResponse> recommendedProducts;
}
