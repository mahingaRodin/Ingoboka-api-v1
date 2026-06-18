package com.ingoboka_api.v1.claim.repositories;

import com.ingoboka_api.v1.claim.models.ClaimDecision;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimDecisionRepository extends JpaRepository<ClaimDecision, UUID> {

    Optional<ClaimDecision> findByClaimId(UUID claimId);
}
