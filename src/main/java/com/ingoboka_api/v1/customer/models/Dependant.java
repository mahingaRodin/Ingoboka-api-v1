package com.ingoboka_api.v1.customer.models;

import com.ingoboka_api.v1.common.enums.DependantRelationship;
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
@Table(name = "dependants")
public class Dependant {

    @Id
    private UUID id;

    @Column(name = "citizen_profile_id", nullable = false)
    private UUID citizenProfileId;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DependantRelationship relationship;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "national_id_hash", length = 128)
    private String nationalIdHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
