package com.ingoboka_api.v1.policy.models;

import com.ingoboka_api.v1.common.enums.PolicyMemberType;
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
@Table(name = "policy_members")
public class PolicyMember {

    @Id
    private UUID id;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false, length = 32)
    private PolicyMemberType memberType;

    @Column(name = "dependant_id")
    private UUID dependantId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 32)
    private String relationship;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
