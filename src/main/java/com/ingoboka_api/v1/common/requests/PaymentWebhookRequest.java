package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentWebhookRequest {

    @NotBlank
    private String providerReference;

    @NotBlank
    private String status;

    @NotBlank
    private String idempotencyKey;
}
