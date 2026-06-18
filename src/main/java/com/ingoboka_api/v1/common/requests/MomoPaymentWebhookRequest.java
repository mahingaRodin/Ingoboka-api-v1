package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MomoPaymentWebhookRequest {

    /** MoMo externalId — maps to our providerReference. */
    @NotBlank
    private String externalId;

    /** MoMo financialTransactionId — used for idempotency when idempotencyKey is omitted. */
    private String financialTransactionId;

    /** SUCCESSFUL, FAILED, or PENDING */
    @NotBlank
    private String status;

    private String idempotencyKey;

    private String amount;

    private String currency;

    private String payerPhone;
}
