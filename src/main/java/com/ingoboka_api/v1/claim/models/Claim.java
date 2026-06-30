package com.ingoboka_api.v1.claim.models;

import com.ingoboka_api.v1.common.enums.ClaimStatus;
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
@Table(name = "claims")
public class Claim {

    @Id
    private UUID id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 64)
    private String claimNumber;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "citizen_profile_id", nullable = false)
    private UUID citizenProfileId;

    @Column(name = "claim_type", nullable = false, length = 64)
    private String claimType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "claimed_amount", precision = 14, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "incident_date")
    private java.time.LocalDate incidentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClaimStatus status = ClaimStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
