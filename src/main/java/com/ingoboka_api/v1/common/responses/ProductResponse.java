package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ProductStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductResponse {
    UUID id;
    UUID organizationId;
    String code;
    String name;
    String description;
    String category;
    ProductStatus status;
    Instant publishedAt;
    String heroImageUrl;
    Instant createdAt;
}
