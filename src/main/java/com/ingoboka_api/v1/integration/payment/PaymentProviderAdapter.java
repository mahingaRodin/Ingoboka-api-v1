package com.ingoboka_api.v1.integration.payment;

import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.common.requests.InitiatePaymentRequest;
import com.ingoboka_api.v1.common.responses.PaymentInitiationResult;

public interface PaymentProviderAdapter {

    String providerCode();

    PaymentInitiationResult initiate(Payment payment, InitiatePaymentRequest request, String payerPhone);
}
