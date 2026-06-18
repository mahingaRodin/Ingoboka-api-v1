package com.ingoboka_api.v1.policy.repositories;

import com.ingoboka_api.v1.policy.models.PolicyDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, UUID> {

    List<PolicyDocument> findByPolicyIdOrderByCreatedAtAsc(UUID policyId);
}
