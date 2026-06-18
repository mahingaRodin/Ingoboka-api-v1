package com.ingoboka_api.v1.enrollment.models;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.enums.PremiumFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "policy_applications")
public class PolicyApplication {

    @Id
    private UUID id;

    @Column(name = "application_reference", nullable = false, unique = true, length = 64)
    private String applicationReference;

    @Column(name = "quote_id")
    private UUID quoteId;

    @Column(name = "citizen_profile_id", nullable = false)
    private UUID citizenProfileId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "product_plan_id", nullable = false)
    private UUID productPlanId;

    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @Column(name = "premium_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal premiumAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "premium_frequency", nullable = false, length = 32)
    private PremiumFrequency premiumFrequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
