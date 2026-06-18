package com.ingoboka_api.v1.billing.services;

import com.ingoboka_api.v1.common.requests.InitiatePaymentRequest;
import com.ingoboka_api.v1.common.requests.PaymentWebhookRequest;
import com.ingoboka_api.v1.common.responses.PaymentResponse;
import java.util.List;
import java.util.UUID;

public interface BillingService {

    PaymentResponse initiatePayment(InitiatePaymentRequest request);

    PaymentResponse processWebhook(String provider, PaymentWebhookRequest request);

    List<PaymentResponse> listMyPayments();

    List<PaymentResponse> listPolicyPayments(UUID policyId);
}
