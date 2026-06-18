package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class GenerateQuoteRequest {

    @NotNull(message = "Product plan ID is required")
    private UUID productPlanId;

    @NotEmpty(message = "At least one assessment answer is required")
    private Map<String, String> answers;
}
