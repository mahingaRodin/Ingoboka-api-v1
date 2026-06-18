package com.ingoboka_api.v1.policy.repositories;

import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.policy.models.Policy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    Optional<Policy> findByApplicationId(UUID applicationId);

    Optional<Policy> findByQrVerificationToken(String token);

    List<Policy> findByCitizenProfileIdOrderByCreatedAtDesc(UUID citizenProfileId);

    List<Policy> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    boolean existsByApplicationId(UUID applicationId);
}
