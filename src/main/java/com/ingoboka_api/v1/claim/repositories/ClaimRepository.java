package com.ingoboka_api.v1.claim.repositories;

import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    List<Claim> findByCitizenProfileIdOrderByCreatedAtDesc(UUID citizenProfileId);

    Page<Claim> findByCitizenProfileIdOrderByCreatedAtDesc(UUID citizenProfileId, Pageable pageable);

    List<Claim> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Page<Claim> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    List<Claim> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, ClaimStatus status);

    Page<Claim> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
            UUID organizationId, ClaimStatus status, Pageable pageable);

    Optional<Claim> findByClaimNumber(String claimNumber);

    long countByOrganizationIdAndStatusIn(UUID organizationId, Collection<ClaimStatus> statuses);

    Page<Claim> findByOrganizationIdAndStatusNotOrderByCreatedAtDesc(
            UUID organizationId, ClaimStatus status, Pageable pageable);

    List<Claim> findByOrganizationIdAndStatusNotOrderByCreatedAtDesc(UUID organizationId, ClaimStatus status);

    @Query(
            """
            SELECT c FROM Claim c
            LEFT JOIN CitizenProfile cp ON cp.id = c.citizenProfileId
            WHERE c.organizationId = :organizationId
            AND c.status <> com.ingoboka_api.v1.common.enums.ClaimStatus.DRAFT
            AND (:status IS NULL OR c.status = :status)
            AND (:search IS NULL OR :search = '' OR LOWER(c.claimNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:district IS NULL OR :district = '' OR cp.district = :district)
            AND (:province IS NULL OR :province = '' OR cp.district IN :districtsInProvince)
            """)
    Page<Claim> findTenantClaimsFiltered(
            @Param("organizationId") UUID organizationId,
            @Param("status") ClaimStatus status,
            @Param("search") String search,
            @Param("district") String district,
            @Param("province") String province,
            @Param("districtsInProvince") Collection<String> districtsInProvince,
            Pageable pageable);
}
