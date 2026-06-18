package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ContractPriceRuleType;
import com.ingoboka_api.v1.common.enums.RateType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContractPriceRuleResponse {
    UUID id;
    UUID organizationId;
    UUID contractId;
    ContractPriceRuleType ruleType;
    RateType rateType;
    BigDecimal rateValue;
    String currency;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
    boolean active;
    Instant createdAt;
}
