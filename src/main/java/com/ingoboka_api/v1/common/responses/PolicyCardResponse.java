package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.PolicyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PolicyCardResponse {

    UUID policyId;
    String policyNumber;
    String holderName;
    String productName;
    String insurerName;
    PolicyStatus status;
    BigDecimal premium;
    BigDecimal coverageAmount;
    String currency;
    LocalDate startDate;
    LocalDate endDate;
    String qrToken;
    String verificationUrl;
}
