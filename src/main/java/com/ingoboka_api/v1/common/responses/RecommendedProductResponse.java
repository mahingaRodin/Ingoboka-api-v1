package com.ingoboka_api.v1.common.responses;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecommendedProductResponse {
    UUID id;
    String name;
    String category;
    BigDecimal startingPremium;
    String currency;
    int matchScore;
    String reason;
}
