package com.ingoboka_api.v1.common.requests;

import lombok.Data;

@Data
public class SandboxPaymentCallbackRequest {

    private String providerReference;
    private String status;
    private String idempotencyKey;
}
