package com.ingoboka_api.v1.policy.repositories;

import com.ingoboka_api.v1.policy.models.PolicyMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyMemberRepository extends JpaRepository<PolicyMember, UUID> {

    List<PolicyMember> findByPolicyIdOrderByCreatedAtAsc(UUID policyId);
}
