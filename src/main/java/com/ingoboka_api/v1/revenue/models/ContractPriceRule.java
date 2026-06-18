package com.ingoboka_api.v1.revenue.models;

import com.ingoboka_api.v1.common.enums.ContractPriceRuleType;
import com.ingoboka_api.v1.common.enums.RateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "contract_price_rules")
public class ContractPriceRule {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 64)
    private ContractPriceRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, length = 32)
    private RateType rateType;

    @Column(name = "rate_value", nullable = false, precision = 14, scale = 4)
    private BigDecimal rateValue;

    @Column(nullable = false, length = 3)
    private String currency = "RWF";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
