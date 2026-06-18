package com.ingoboka_api.v1.customer.models;

import com.ingoboka_api.v1.common.enums.KycStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "citizen_profiles")
public class CitizenProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "national_id_hash", length = 128)
    private String nationalIdHash;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 16)
    private String gender;

    @Column(name = "address_line")
    private String addressLine;

    @Column(length = 120)
    private String district;

    @Column(nullable = false, length = 2)
    private String country = "RW";

    @Column(length = 120)
    private String occupation;

    @Column(name = "preferred_language", nullable = false, length = 8)
    private String preferredLanguage = "en";

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 32)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
