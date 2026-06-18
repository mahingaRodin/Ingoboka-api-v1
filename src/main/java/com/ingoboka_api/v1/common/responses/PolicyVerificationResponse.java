package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.PolicyStatus;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PolicyVerificationResponse {
    String policyNumber;
    PolicyStatus status;
    LocalDate startDate;
    LocalDate endDate;
    String insurerCode;
    boolean valid;
}
