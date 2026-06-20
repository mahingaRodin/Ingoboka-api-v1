package com.ingoboka_api.v1.policy.repositories;

import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.policy.models.Policy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    Optional<Policy> findByApplicationId(UUID applicationId);

    Optional<Policy> findByQrVerificationToken(String token);

    List<Policy> findByCitizenProfileIdOrderByCreatedAtDesc(UUID citizenProfileId);

    Page<Policy> findByCitizenProfileIdOrderByCreatedAtDesc(UUID citizenProfileId, Pageable pageable);

    List<Policy> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Page<Policy> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    boolean existsByApplicationId(UUID applicationId);

    long countByOrganizationIdAndStatus(UUID organizationId, PolicyStatus status);

    @Query(
            "SELECT COUNT(DISTINCT p.citizenProfileId) FROM Policy p WHERE p.organizationId = :organizationId AND p.status = :status")
    long countDistinctCitizensByOrganizationIdAndStatus(
            @Param("organizationId") UUID organizationId, @Param("status") PolicyStatus status);

    List<Policy> findByStatus(PolicyStatus status);

    List<Policy> findByStatusAndEndDateBefore(PolicyStatus status, LocalDate date);
}
