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

    Page<Policy> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByApplicationId(UUID applicationId);

    long countByOrganizationIdAndStatus(UUID organizationId, PolicyStatus status);

    @Query(
            "SELECT COUNT(DISTINCT p.citizenProfileId) FROM Policy p WHERE p.organizationId = :organizationId AND p.status = :status")
    long countDistinctCitizensByOrganizationIdAndStatus(
            @Param("organizationId") UUID organizationId, @Param("status") PolicyStatus status);

    List<Policy> findByStatus(PolicyStatus status);

    List<Policy> findByStatusAndEndDateBefore(PolicyStatus status, LocalDate date);

    @Query(
            """
            SELECT cp.district, COUNT(DISTINCT p.citizenProfileId)
            FROM Policy p
            JOIN CitizenProfile cp ON cp.id = p.citizenProfileId
            WHERE p.organizationId = :organizationId AND p.status = :status AND cp.district IS NOT NULL
            GROUP BY cp.district
            """)
    List<Object[]> countEnrolledByDistrict(
            @Param("organizationId") UUID organizationId, @Param("status") PolicyStatus status);

    @Query(
            """
            SELECT prod.name, COUNT(DISTINCT p.citizenProfileId)
            FROM Policy p
            JOIN ProductPlan plan ON plan.id = p.productPlanId
            JOIN InsuranceProduct prod ON prod.id = plan.productId
            WHERE p.organizationId = :organizationId AND p.status = :status
            GROUP BY prod.name
            ORDER BY COUNT(DISTINCT p.citizenProfileId) DESC
            """)
    List<Object[]> countEnrolledByProduct(
            @Param("organizationId") UUID organizationId, @Param("status") PolicyStatus status);
}
