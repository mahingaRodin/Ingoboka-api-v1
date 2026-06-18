package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.ContractPriceRuleType;
import com.ingoboka_api.v1.common.enums.RateType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateContractPriceRuleRequest {

    private UUID contractId;

    @NotNull
    private ContractPriceRuleType ruleType;

    @NotNull
    private RateType rateType;

    @NotNull
    private BigDecimal rateValue;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
