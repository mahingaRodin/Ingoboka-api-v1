package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class InitiatePaymentRequest {

    @NotNull(message = "Policy ID is required")
    private UUID policyId;

    private BigDecimal amount;

    private String idempotencyKey;

    /** SANDBOX (default) or MOMO_SANDBOX */
    private String provider;

    /** Required for MOMO_SANDBOX — Rwanda MSISDN e.g. 2507XXXXXXXX */
    private String payerPhone;
}
