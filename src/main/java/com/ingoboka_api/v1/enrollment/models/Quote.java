package com.ingoboka_api.v1.enrollment.models;

import com.ingoboka_api.v1.common.enums.PremiumFrequency;
import com.ingoboka_api.v1.common.enums.QuoteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    private UUID id;

    @Column(name = "quote_reference", nullable = false, unique = true, length = 64)
    private String quoteReference;

    @Column(name = "citizen_profile_id", nullable = false)
    private UUID citizenProfileId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "product_plan_id", nullable = false)
    private UUID productPlanId;

    @Column(name = "premium_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal premiumAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "premium_frequency", nullable = false, length = 32)
    private PremiumFrequency premiumFrequency;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuoteStatus status = QuoteStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
