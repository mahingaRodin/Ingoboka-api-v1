package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.PaymentStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentStatusResponse {

    UUID id;
    PaymentStatus status;
    String paymentReference;
}
