package com.ingoboka_api.v1.product.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "eligibility_rules")
public class EligibilityRule {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "rule_type", nullable = false, length = 64)
    private String ruleType = "AGE_RANGE";

    @Column(name = "rule_value", columnDefinition = "TEXT")
    private String ruleValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
