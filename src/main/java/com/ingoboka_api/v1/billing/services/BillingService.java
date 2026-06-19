package com.ingoboka_api.v1.billing.services;

import com.ingoboka_api.v1.common.requests.InitiatePaymentRequest;
import com.ingoboka_api.v1.common.requests.MomoPaymentWebhookRequest;
import com.ingoboka_api.v1.common.requests.PaymentWebhookRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PaymentResponse;
import com.ingoboka_api.v1.common.responses.PaymentStatusResponse;
import java.util.UUID;

public interface BillingService {

    PaymentResponse initiatePayment(InitiatePaymentRequest request);

    PaymentStatusResponse getPaymentStatus(UUID paymentId);

    PaymentResponse processWebhook(String provider, PaymentWebhookRequest request);

    PaymentResponse processMomoWebhook(MomoPaymentWebhookRequest request);

    PageResponse<PaymentResponse> listMyPayments(int page, int size);

    PageResponse<PaymentResponse> listPolicyPayments(UUID policyId, int page, int size);
}
