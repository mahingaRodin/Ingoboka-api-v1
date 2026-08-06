package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NeedsAssessmentPreferencesResponse {
    boolean completed;
    Instant completedAt;
    Map<String, Object> preferences;
    List<String> recommendedCategories;
    List<RecommendedProductResponse> recommendedProducts;
}
