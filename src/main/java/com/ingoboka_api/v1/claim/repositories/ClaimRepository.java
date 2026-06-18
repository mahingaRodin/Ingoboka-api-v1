package com.ingoboka_api.v1.claim.repositories;

import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    List<Claim> findByCitizenProfileIdOrderByCreatedAtDesc(UUID citizenProfileId);

    List<Claim> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Claim> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, ClaimStatus status);

    Optional<Claim> findByClaimNumber(String claimNumber);
}
