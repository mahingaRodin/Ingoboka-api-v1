package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.ClaimDecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class RecordClaimDecisionRequest {

    @NotNull
    private ClaimDecisionType decision;

    private BigDecimal approvedAmount;

    @NotBlank
    private String reason;
}
