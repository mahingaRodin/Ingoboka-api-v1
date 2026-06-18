package com.ingoboka_api.v1.claim.repositories;

import com.ingoboka_api.v1.claim.models.ClaimAppeal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimAppealRepository extends JpaRepository<ClaimAppeal, UUID> {

    List<ClaimAppeal> findByClaimIdOrderBySubmittedAtDesc(UUID claimId);
}
