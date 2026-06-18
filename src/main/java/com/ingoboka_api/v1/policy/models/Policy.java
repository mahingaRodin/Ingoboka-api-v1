package com.ingoboka_api.v1.policy.models;

import com.ingoboka_api.v1.common.enums.PolicyStatus;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "policies")
public class Policy {

    @Id
    private UUID id;

    @Column(name = "policy_number", nullable = false, unique = true, length = 64)
    private String policyNumber;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(name = "citizen_profile_id", nullable = false)
    private UUID citizenProfileId;

    @Column(name = "product_plan_id", nullable = false)
    private UUID productPlanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicyStatus status = PolicyStatus.PENDING_PAYMENT;

    @Column(name = "premium_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal premiumAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "premium_frequency", nullable = false, length = 32)
    private PremiumFrequency premiumFrequency;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "qr_verification_token", nullable = false, unique = true, length = 128)
    private String qrVerificationToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
