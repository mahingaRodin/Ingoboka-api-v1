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
}
