package com.ingoboka_api.v1.integration.payment;

import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.common.requests.InitiatePaymentRequest;
import com.ingoboka_api.v1.common.responses.PaymentInitiationResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SandboxPaymentAdapter implements PaymentProviderAdapter {

    public static final String CODE = "SANDBOX";

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public PaymentInitiationResult initiate(Payment payment, InitiatePaymentRequest request, String payerPhone) {
        return PaymentInitiationResult.builder()
                .providerReference("PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .instructions("Complete payment via POST /api/payments/webhooks/sandbox")
                .build();
    }
}
