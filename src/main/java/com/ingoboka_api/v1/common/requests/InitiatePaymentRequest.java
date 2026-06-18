package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class InitiatePaymentRequest {

    @NotNull(message = "Policy ID is required")
    private UUID policyId;

    private String idempotencyKey;
}
