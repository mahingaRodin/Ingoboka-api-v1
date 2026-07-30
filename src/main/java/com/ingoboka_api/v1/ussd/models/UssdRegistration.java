package com.ingoboka_api.v1.ussd.models;

import com.ingoboka_api.v1.common.enums.UssdRegistrationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ussd_registrations")
public class UssdRegistration {

    @Id
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 32)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, length = 16)
    private UssdRegistrationType registrationType;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "business_name")
    private String businessName;

    @Column(length = 120)
    private String district;

    @Column(nullable = false, length = 8)
    private String language = "rw";

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "reference_code", nullable = false, unique = true, length = 32)
    private String referenceCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
