package com.ingoboka_api.v1.integration.payment;

import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.InitiatePaymentRequest;
import com.ingoboka_api.v1.common.responses.PaymentInitiationResult;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MomoSandboxPaymentAdapter implements PaymentProviderAdapter {

    public static final String CODE = "MOMO_SANDBOX";

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public PaymentInitiationResult initiate(Payment payment, InitiatePaymentRequest request, String payerPhone) {
        String phone = StringUtils.hasText(request.getPayerPhone()) ? request.getPayerPhone() : payerPhone;
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException("Payer phone number is required for MoMo payments");
        }

        String externalId = "MOMO-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String financialTransactionId = "FTX-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        return PaymentInitiationResult.builder()
                .providerReference(externalId)
                .externalTransactionId(financialTransactionId)
                .payerPhone(phone)
                .instructions(
                        "Sandbox MoMo: approve payment on phone " + phone
                                + ". Callback via POST /api/payments/webhooks/momo with externalId="
                                + externalId)
                .build();
    }
}
