package com.ingoboka_api.v1.common.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentInitiationResult {
    String providerReference;
    String externalTransactionId;
    String payerPhone;
    String instructions;
}
